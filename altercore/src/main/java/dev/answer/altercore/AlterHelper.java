/*
 * Copyright (C) 2026 AnswerDev
 * Licensed under the GNU General Public License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.gnu.org/licenses/gpl-3.0.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.answer.altercore;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import dev.answer.altercore.callback.AfterCallback;
import dev.answer.altercore.callback.BeforeCallback;
import dev.answer.altercore.callback.ReplaceCallback;

/**
 * Fluent helper that combines Reflector-style class/member lookup with
 * {@link AlterCore}'s before/after/replace hooking API, so a hook target can
 * be found and wired up in a single chained expression instead of
 * separately reflecting a {@link Member} and then calling into AlterCore.
 *
 * <p>The chain has two phases that alternate:
 * <ol>
 *   <li><b>select</b> — {@link #method}, {@link #methods},
 *       {@link #constructor} or {@link #constructors} resolve
 *       one or more {@link Member}s and remember them as the "current
 *       selection".</li>
 *   <li><b>hook</b> — {@link #before}, {@link #after}, {@link #replace} or
 *       {@link #doNothing} register a callback on every member in the
 *       current selection.</li>
 * </ol>
 *
 * <pre>{@code
 * // hook a single method
 * AlterHelper.on("com.example.Foo")
 *         .method("bar", int.class)
 *         .before(p -> Log.d("hook", "bar(" + p.args[0] + ") called"))
 *         .after(p -> p.setResult(((int) p.getResult()) + 1));
 *
 * // hook every overload named "bar" in one go
 * AlterHelper handle = AlterHelper.on(Foo.class)
 *         .methods("bar")
 *         .replace(p -> null);
 *
 * // later, tear down every hook this instance installed
 * handle.unhookRegistered();
 *
 * // constructors work the same way
 * AlterHelper.on(Foo.class).constructor(String.class).after(p -> {...});
 *
 * // fields are reflective get/set only — AlterCore can't hook a field
 * // access, only a method/constructor call — so this skips the
 * // select/hook phases entirely and returns a handle directly.
 * Object value = AlterHelper.on(Foo.class).field("mSecret").get(fooInstance);
 * }</pre>
 *
 * <p>A single instance can be reused to hook several different members of
 * the same class one after another — each select call simply replaces the
 * current selection, it doesn't forget members hooked by an earlier one:
 * <pre>{@code
 * AlterHelper h = AlterHelper.on(Foo.class);
 * h.method("a").before(p -> {...});
 * h.method("b").after(p -> {...});
 * // unhookRegistered() removes BOTH the "a" and the "b" hook.
 * h.unhookRegistered();
 * }</pre>
 *
 * <p>Not thread-safe: a single {@code AlterHelper} instance is meant to be
 * built and consumed on one thread, the way a local variable would be.
 * Concurrency safety for the actual hooking is provided by {@link AlterCore}
 * itself.
 */
public class AlterHelper {

    private final Class<?> type;

    /** Members resolved by the most recent select* call; the target of the next hook* call. */
    private final List<Member> selectedMembers = new ArrayList<>();

    /** Every token handed back by AlterCore for a hook registered through this instance. */
    private final List<AlterCore.CallbackToken> registeredTokens = new CopyOnWriteArrayList<>();

    private AlterHelper(Class<?> type) {
        this.type = type;
    }

    /**
     * Resolves {@code className} via this class's classloader, without
     * running the target class's {@code <clinit>}.
     *
     * @param className the fully-qualified name of the class to operate on
     * @return a new helper scoped to the resolved class
     * @throws ClassNotFoundException if no class with that name can be found
     */
    public static AlterHelper init(String className) throws ClassNotFoundException {
        return init(className, AlterHelper.class.getClassLoader());
    }

    /**
     * Resolves {@code className} via the given {@code loader}, without
     * running the target class's {@code <clinit>}.
     *
     * @param className the fully-qualified name of the class to operate on
     * @param loader    the classloader to resolve {@code className} with
     * @return a new helper scoped to the resolved class
     * @throws ClassNotFoundException if no class with that name can be found
     *                                via {@code loader}
     */
    public static AlterHelper init(String className, ClassLoader loader) throws ClassNotFoundException {
        return init(Class.forName(className, false, loader));
    }

    /**
     * Wraps an already-resolved class.
     *
     * @param clazz the class to operate on
     * @return a new helper scoped to {@code clazz}
     * @throws NullPointerException if {@code clazz} is {@code null}
     */
    public static AlterHelper init(Class<?> clazz) {
        if (clazz == null) throw new NullPointerException("clazz == null");
        return new AlterHelper(clazz);
    }
    

    /**
     * Selects a single method by name and exact parameter types, searching
     * this class and its superclasses in that order (the first match wins,
     * same as normal method resolution). Replaces the current selection.
     *
     * @param name       the method name
     * @param paramTypes the method's exact parameter types, in order
     * @return this helper, for chaining into {@link #before}/{@link #after}/
     * {@link #replace}/{@link #doNothing}
     * @throws IllegalArgumentException if no such method is declared by this
     *                                  class or any of its superclasses
     */
    public AlterHelper method(String name, Class<?>... paramTypes) {
        Method m = findDeclaredMethod(type, name, paramTypes);
        if (m == null) {
            throw new IllegalArgumentException(
                    "No method " + name + signatureOf(paramTypes) + " in " + type);
        }
        m.setAccessible(true);
        replaceSelection(m);
        return this;
    }

    /**
     * Selects every declared method whose name matches any of {@code names},
     * across the class hierarchy — i.e. all overloads of each given name,
     * for every name given, in one go. Useful when the same before/after/
     * replace behavior should apply regardless of which overload (or which
     * of several differently-named methods) gets called. Replaces the
     * current selection.
     *
     * @param names one or more method names to match, regardless of parameter types
     * @return this helper, for chaining into {@link #before}/{@link #after}/
     * {@link #replace}/{@link #doNothing}
     * @throws IllegalArgumentException if {@code names} is empty, or if none
     *                                  of the given names is declared by this class or any of its superclasses
     */
    public AlterHelper methods(String... names) {
        if (names == null || names.length == 0) {
            throw new IllegalArgumentException("names must not be empty");
        }
        List<Member> matches = new ArrayList<>();
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (containsName(names, m.getName())) {
                    m.setAccessible(true);
                    matches.add(m);
                }
            }
        }
        if (matches.isEmpty()) {
            throw new IllegalArgumentException(
                    "No method named any of " + java.util.Arrays.toString(names) + " in " + type);
        }
        replaceSelection(matches);
        return this;
    }

    /**
     * @param names     the candidate names to search
     * @param candidate the name to look for
     * @return {@code true} if {@code candidate} equals any element of {@code names}
     */
    private static boolean containsName(String[] names, String candidate) {
        for (String n : names) {
            if (n.equals(candidate)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Selects a single declared constructor by its parameter types. Replaces
     * the current selection.
     *
     * @param paramTypes the constructor's exact parameter types, in order
     * @return this helper, for chaining into {@link #before}/{@link #after}/
     * {@link #replace}/{@link #doNothing}
     * @throws IllegalArgumentException if this class declares no constructor
     *                                  with that parameter list
     */
    public AlterHelper constructor(Class<?>... paramTypes) {
        try {
            Constructor<?> c = type.getDeclaredConstructor(paramTypes);
            c.setAccessible(true);
            replaceSelection(c);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                    "No constructor " + signatureOf(paramTypes) + " in " + type, e);
        }
        return this;
    }

    /**
     * Selects every declared constructor of this class (no overload
     * filtering, since constructors are unnamed). Replaces the current
     * selection.
     *
     * @return this helper, for chaining into {@link #before}/{@link #after}/
     * {@link #replace}/{@link #doNothing}
     * @throws IllegalArgumentException if this class declares no constructors
     */
    public AlterHelper constructors() {
        Constructor<?>[] declared = type.getDeclaredConstructors();
        if (declared.length == 0) {
            throw new IllegalArgumentException("No constructors in " + type);
        }
        List<Member> matches = new ArrayList<>();
        for (Constructor<?> c : declared) {
            c.setAccessible(true);
            matches.add(c);
        }
        replaceSelection(matches);
        return this;
    }

    /**
     * Adds a single declared constructor to the current selection, without
     * discarding whatever was selected before — unlike
     * {@link #constructor}/{@link #constructors}, which always
     * replace it. Lets you build up a selection across multiple calls:
     * <pre>{@code
     * AlterHelper.on(Foo.class)
     *         .constructor(String.class)
     *         .andConstructor(int.class)
     *         .andConstructor(String.class, int.class)
     *         .after(ctorCallback); // all three constructors are hooked
     * }</pre>
     * If nothing has been selected yet, this behaves like
     * {@link #constructor}.
     *
     * @param paramTypes the constructor's exact parameter types, in order
     * @return this helper, for chaining into further {@code and*}/select
     * calls or into {@link #before}/{@link #after}/{@link #replace}/{@link #doNothing}
     * @throws IllegalArgumentException if this class declares no constructor
     *                                  with that parameter list
     */
    public AlterHelper andConstructor(Class<?>... paramTypes) {
        try {
            Constructor<?> c = type.getDeclaredConstructor(paramTypes);
            c.setAccessible(true);
            addToSelection(c);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                    "No constructor " + signatureOf(paramTypes) + " in " + type, e);
        }
        return this;
    }

    /** Appends a single member to the current selection, keeping whatever was already there. */
    private void addToSelection(Member single) {
        selectedMembers.add(single);
    }

    /**
     * Looks up a field by name (searching superclasses too) and returns a
     * standalone {@link FieldHandle} for reflective get/set. This bypasses
     * the select/hook chain entirely and does not touch the current
     * selection: {@link AlterCore} hooks method and constructor *calls*, and
     * a field read/write isn't a call, so there's nothing here to hook —
     * only to access.
     *
     * @param name the field name
     * @return a handle for getting/setting that field
     * @throws IllegalArgumentException if no such field is declared by this
     *                                  class or any of its superclasses
     */
    public FieldHandle field(String name) {
        Field f = findDeclaredField(type, name);
        if (f == null) {
            throw new IllegalArgumentException("No field " + name + " in " + type);
        }
        f.setAccessible(true);
        return new FieldHandle(f);
    }
    
    /**
     * Registers {@code callback} to run before every currently-selected
     * member, via {@link AlterCore#before}.
     *
     * @param callback the callback to run before each selected member is called
     * @return this helper, for chaining further hook calls or a subsequent select call
     * @throws IllegalStateException if nothing has been selected yet
     */
    public AlterHelper before(BeforeCallback callback) {
        for (Member member : requireSelection()) {
            addTokenIfPresent(AlterCore.before(member, callback));
        }
        return this;
    }

    /**
     * Registers {@code callback} to run after every currently-selected
     * member, via {@link AlterCore#after}.
     *
     * @param callback the callback to run after each selected member is called
     * @return this helper, for chaining further hook calls or a subsequent select call
     * @throws IllegalStateException if nothing has been selected yet
     */
    public AlterHelper after(AfterCallback callback) {
        for (Member member : requireSelection()) {
            addTokenIfPresent(AlterCore.after(member, callback));
        }
        return this;
    }

    /**
     * Installs {@code callback} as the active replacement for every
     * currently-selected member, via {@link AlterCore#replace}. As with
     * {@link AlterCore#replace}, this supersedes any previously-installed
     * replace callback on the same member rather than stacking with it.
     *
     * @param callback the callback that replaces each selected member's implementation
     * @return this helper, for chaining further hook calls or a subsequent select call
     * @throws IllegalStateException if nothing has been selected yet
     */
    public AlterHelper replace(ReplaceCallback callback) {
        for (Member member : requireSelection()) {
            addTokenIfPresent(AlterCore.replace(member, callback));
        }
        return this;
    }

    /**
     * Convenience for {@code replace(AlterCore.DO_NOTHING)} on every
     * currently-selected member.
     *
     * @return this helper, for chaining further hook calls or a subsequent select call
     * @throws IllegalStateException if nothing has been selected yet
     */
    public AlterHelper doNothing() {
        for (Member member : requireSelection()) {
            addTokenIfPresent(AlterCore.doNothing(member));
        }
        return this;
    }
    

    /**
     * Removes only the callbacks registered through this instance so far,
     * across every member it has ever selected (not just the current
     * selection). Idempotent — safe to call more than once, and safe even
     * if some of those hooks were already torn down some other way.
     */
    public void unhookRegistered() {
        for (AlterCore.CallbackToken token : registeredTokens) {
            token.remove();
        }
        registeredTokens.clear();
    }

    /**
     * Forcibly tears down the current selection via {@link AlterCore#unHook},
     * removing every callback registered on those members from anywhere in
     * the process — not just the ones added through this instance. Prefer
     * {@link #unhookRegistered()} unless that's actually what's needed.
     */
    public void unhookSelected() {
        for (Member member : selectedMembers) {
            AlterCore.unHook(member);
        }
    }

    /**
     * The members resolved by the most recent select* call.
     *
     * @return an unmodifiable snapshot of the current selection
     */
    public List<Member> selectedMembers() {
        return List.copyOf(selectedMembers);
    }

    /**
     * Every {@link AlterCore.CallbackToken} registered through this instance
     * so far.
     *
     * @return an unmodifiable snapshot of the tokens registered by this instance
     */
    public List<AlterCore.CallbackToken> registeredTokens() {
        return List.copyOf(registeredTokens);
    }
    

    /** Replaces the current selection with a single member. */
    private void replaceSelection(Member single) {
        selectedMembers.clear();
        selectedMembers.add(single);
    }

    /** Replaces the current selection with {@code members}. */
    private void replaceSelection(List<Member> members) {
        selectedMembers.clear();
        selectedMembers.addAll(members);
    }

    /**
     * @return the current selection
     * @throws IllegalStateException if nothing has been selected yet
     */
    private List<Member> requireSelection() {
        if (selectedMembers.isEmpty()) {
            throw new IllegalStateException(
                    "Nothing selected — call method()/methods()/"
                            + "constructor()/constructors() before hooking");
        }
        return selectedMembers;
    }

    /**
     * Records {@code token} in {@link #registeredTokens}, unless it's
     * {@code null}. {@code AlterCore.before/after/replace} return {@code
     * null} on registration failure (e.g. the underlying hook install
     * failed) rather than throwing, so this filters those out instead of
     * tracking a stale null token.
     *
     * @param token the token to record, or {@code null} if registration failed
     */
    private void addTokenIfPresent(AlterCore.CallbackToken token) {
        if (token != null) {
            registeredTokens.add(token);
        }
    }

    /**
     * Searches {@code type} and its superclasses, in that order, for a
     * declared method matching {@code name} and {@code paramTypes} exactly.
     *
     * @param type       the class to start searching from
     * @param name       the method name
     * @param paramTypes the exact parameter types, in order
     * @return the matching method, or {@code null} if none is found
     */
    private static Method findDeclaredMethod(Class<?> type, String name, Class<?>... paramTypes) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try {
                return c.getDeclaredMethod(name, paramTypes);
            } catch (NoSuchMethodException ignored) {
                // not declared here — keep walking up to the superclass
            }
        }
        return null;
    }

    /**
     * Searches {@code type} and its superclasses, in that order, for a
     * declared field named {@code name}.
     *
     * @param type the class to start searching from
     * @param name the field name
     * @return the matching field, or {@code null} if none is found
     */
    private static Field findDeclaredField(Class<?> type, String name) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                // not declared here — keep walking up to the superclass
            }
        }
        return null;
    }

    /**
     * Formats {@code paramTypes} as a Java-style parameter list, e.g.
     * {@code "(int, java.lang.String)"}, for use in error messages.
     *
     * @param paramTypes the parameter types to format
     * @return the formatted parameter list, including the enclosing parentheses
     */
    private static String signatureOf(Class<?>[] paramTypes) {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < paramTypes.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(paramTypes[i] == null ? "null" : paramTypes[i].getName());
        }
        return sb.append(")").toString();
    }

    /**
     * Standalone reflective get/set wrapper for a field resolved via
     * {@link AlterHelper#field(String)}. Deliberately not routed through
     * {@link AlterCore} — field access isn't a method call, so there's
     * nothing for AlterCore's before/after/replace hooks to attach to here.
     */
    public static final class FieldHandle {
        private final Field field;

        private FieldHandle(Field field) {
            this.field = field;
        }

        /**
         * Reads the field's current value.
         *
         * @param instance the object to read from, or {@code null} for a static field
         * @param <R>      the expected value type, inferred at the call site
         * @return the field's current value
         * @throws RuntimeException wrapping the underlying {@link IllegalAccessException}
         *                          if the read fails
         */
        @SuppressWarnings("unchecked")
        public <R> R get(Object instance) {
            try {
                return (R) field.get(instance);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to read field " + field, e);
            }
        }

        /**
         * Writes a new value to the field.
         *
         * @param instance the object to write to, or {@code null} for a static field
         * @param value    the value to store
         * @return this handle, for chaining
         * @throws RuntimeException wrapping the underlying {@link IllegalAccessException}
         *                          if the write fails
         */
        public FieldHandle set(Object instance, Object value) {
            try {
                field.set(instance, value);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to write field " + field, e);
            }
            return this;
        }

        /**
         * @return the underlying {@link Field}, already made accessible
         */
        public Field raw() {
            return field;
        }
    }
}