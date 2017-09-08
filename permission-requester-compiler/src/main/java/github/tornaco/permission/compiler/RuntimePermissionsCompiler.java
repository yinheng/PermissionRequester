package github.tornaco.permission.compiler;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;

import github.tornaco.permission.compiler.common.Collections;
import github.tornaco.permission.compiler.common.Logger;
import github.tornaco.permission.compiler.common.MoreElements;
import github.tornaco.permission.compiler.common.SettingsProvider;
import github.tornaco.permission.requester.RequiresPermission;
import github.tornaco.permission.requester.RuntimePermissions;

import static github.tornaco.permission.compiler.SourceFiles.writeSourceFile;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;

/**
 * Created by guohao4 on 2017/9/6.
 * Email: Tornaco@163.com
 */
@SupportedAnnotationTypes("github.tornaco.permission.requester.RuntimePermissions")
public class RuntimePermissionsCompiler extends AbstractProcessor {

    private static final Map<String, Type> PRIMITIVE_TYPES = Maps.newHashMap();

    private static final AtomicInteger REQUEST_CODE = new AtomicInteger(0x999);

    private static final boolean DEBUG = true;

    private ErrorReporter mErrorReporter;
    private Types mTypeUtils;

    static {
        PRIMITIVE_TYPES.put("int", int.class);
        PRIMITIVE_TYPES.put("boolean", boolean.class);
        PRIMITIVE_TYPES.put("float", float.class);
        PRIMITIVE_TYPES.put("double", double.class);
        PRIMITIVE_TYPES.put("long", long.class);
        PRIMITIVE_TYPES.put("char", char.class);
        PRIMITIVE_TYPES.put("byte", byte.class);
        PRIMITIVE_TYPES.put("short", short.class);
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        mErrorReporter = new ErrorReporter(processingEnvironment);
        mTypeUtils = processingEnvironment.getTypeUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        Logger.debug("process @RuntimePermissions");

        Collection<? extends Element> annotatedElements =
                roundEnvironment.getElementsAnnotatedWith(RuntimePermissions.class);

        List<TypeElement> types = new ImmutableList.Builder<TypeElement>()
                .addAll(ElementFilter.typesIn(annotatedElements))
                .build();

        Logger.debug("Will process %d types of @RuntimePermissions", types.size());

        for (TypeElement type : types) {
            processType(type);
        }

        return true;
    }

    private void processType(TypeElement type) {
        RuntimePermissions annotation = type.getAnnotation(RuntimePermissions.class);
        if (annotation == null) {
            Logger.report("@RuntimePermissions annotation is null on Type %s", type);
            return;
        }
        if (type.getKind() != ElementKind.CLASS) {
            mErrorReporter.abortWithError("@RuntimePermissions" + " only applies to class", type);
        }

        NestingKind nestingKind = type.getNestingKind();
        if (nestingKind != NestingKind.TOP_LEVEL) {
            mErrorReporter.abortWithError("@RuntimePermissions" + " only applies to top level class", type);
        }

        checkModifiersIfNested(type);

        // get the fully-qualified class name
        String fqClassName = generatedSubclassName(type, 0, "PermissionRequester");
        // class name
        String className = CompilerUtil.simpleNameOf(fqClassName);
        // Create source.
        String source = generateClass(type, className, type.getSimpleName().toString(), false);

        source = Reformatter.fixup(source);
        writeSourceFile(processingEnv, fqClassName, source, type);
    }

    private String generatedSubclassName(TypeElement type, int depth, String subFix) {
        return generatedClassName(type, null, Strings.repeat("$", depth) + subFix);
    }

    private String generatedClassName(TypeElement type, String prefix, String subFix) {
        String name = type.getSimpleName().toString();
        while (type.getEnclosingElement() instanceof TypeElement) {
            type = (TypeElement) type.getEnclosingElement();
            name = type.getSimpleName() + "_" + name;
        }
        String pkg = CompilerUtil.packageNameOf(type);
        String dot = Strings.isNullOrEmpty(pkg) ? "" : ".";
        String prefixChecked = Strings.isNullOrEmpty(prefix) ? "" : prefix;
        String subFixChecked = Strings.isNullOrEmpty(subFix) ? "" : subFix;
        return pkg + dot + prefixChecked + name + subFixChecked;
    }


    private String generateClass(TypeElement type, String className, String ifaceToImpl, boolean isFinal) {
        if (type == null) {
            mErrorReporter.abortWithError("generateClass was invoked with null type", null);
            return null;
        }
        if (className == null) {
            mErrorReporter.abortWithError("generateClass was invoked with null class name", type);
            return null;
        }
        if (ifaceToImpl == null) {
            mErrorReporter.abortWithError("generateClass was invoked with null iface", type);
            return null;
        }

        String pkg = CompilerUtil.packageNameOf(type);

        ClassName strClz = ClassName.get("java.lang", "Runnable");
        ClassName intClz = ClassName.get("java.lang", "Integer");
        ClassName mapClz = ClassName.get("java.util", "Map");
        TypeName mapOfString = ParameterizedTypeName.get(mapClz, intClz, strClz);
        FieldSpec onGrantMethodsMap = FieldSpec.builder(mapOfString, "ON_GRANT_METHODS_MAP")
                .addModifiers(Modifier.STATIC, Modifier.FINAL, Modifier.PRIVATE).build();

        FieldSpec onDenyMethodsMap = FieldSpec.builder(mapOfString, "ON_DENY_METHODS_MAP")
                .addModifiers(Modifier.STATIC, Modifier.FINAL, Modifier.PRIVATE).build();

        TypeSpec.Builder subClass = TypeSpec.classBuilder(className)
                .addField(boolean.class, "DEBUG", Modifier.STATIC, Modifier.FINAL, Modifier.PRIVATE)
                .addStaticBlock(CodeBlock.of("DEBUG = " + DEBUG + ";\n"))
                .addField(onGrantMethodsMap)
                .addField(onDenyMethodsMap)
                .addStaticBlock(CodeBlock.of("ON_GRANT_METHODS_MAP = new $T<>();\n", HashMap.class))
                .addStaticBlock(CodeBlock.of("ON_DENY_METHODS_MAP = new $T<>();\n", HashMap.class))
                .addMethods(createMethodSpecs(type));

        // Add type params.
        List l = type.getTypeParameters();
        Collections.consumeRemaining(l, o -> {
            TypeParameterElement typeParameterElement = (TypeParameterElement) o;
            subClass.addTypeVariable(TypeVariableName.get(typeParameterElement.toString()));
        });

        if (isFinal) subClass.addModifiers(FINAL);

        JavaFile javaFile = JavaFile.builder(pkg, subClass.build())
                .addFileComment(SettingsProvider.FILE_COMMENT)
                .skipJavaLangImports(true)
                .build();
        return javaFile.toString();
    }

    private Iterable<MethodSpec> createMethodSpecs(TypeElement typeElement) {
        List<MethodSpec> methodSpecs = new ArrayList<>();

        methodSpecs.add(createOnPermissionRequestResultMethod());

        List<? extends Element> enclosedElements = typeElement.getEnclosedElements();

        for (Element e : enclosedElements) {
            Logger.debug("enclosedElements: %s", e);
            if (e.getKind() == ElementKind.METHOD) {
                RequiresPermission requiresPermission = e.getAnnotation(RequiresPermission.class);
                RequiresPermission.Before before = e.getAnnotation(RequiresPermission.Before.class);
                RequiresPermission.OnDenied onDenied = e.getAnnotation(RequiresPermission.OnDenied.class);
                if (requiresPermission != null) {
                    Logger.debug("RequiresPermission found @:" + e);
                    try {
                        methodSpecs.add(createMethodForRequiresPermission(typeElement, e,
                                before,
                                onDenied,
                                requiresPermission));
                    } catch (Throwable t) {
                        StringBuilder stacks = new StringBuilder();
                        for (StackTraceElement se : t.getStackTrace()) {
                            stacks.append(se.toString()).append("\n");
                        }
                        mErrorReporter.abortWithError(stacks.toString(), e);
                    }
                }
            }
        }
        return methodSpecs;
    }

    private MethodSpec createOnPermissionRequestResultMethod() {
        MethodSpec.Builder methodSpecBuilder = MethodSpec.methodBuilder("onRequestPermissionsResult")
                .addParameter(TypeName.INT, "requestCode")
                .addParameter(String[].class, "permissions")
                .addParameter(int[].class, "grantResults")
                .addStatement("if (!ON_GRANT_METHODS_MAP.containsKey(requestCode)) return")
                .addStatement("$T<String> permissionsNotGrantList = new $T<String>(permissions.length)",
                        List.class, ArrayList.class)
                .beginControlFlow(" for (int i = 0; i < grantResults.length; i++)")
                .beginControlFlow("if (grantResults[i] != android.content.pm.PackageManager.PERMISSION_GRANTED)")
                .addStatement("permissionsNotGrantList.add(permissions[i])")
                .endControlFlow()
                .endControlFlow()
                .beginControlFlow(" if (permissionsNotGrantList.size() == 0)")
                .addCode("// Now call his method.\n")
                .addStatement("Runnable r = ON_GRANT_METHODS_MAP.remove(requestCode)")
                .addStatement(" if (r != null) r.run()")
                .addStatement("return")
                .endControlFlow()
                .addStatement("Runnable r2 = ON_DENY_METHODS_MAP.remove(requestCode)")
                .addStatement(" if (r2 != null) r2.run()")
                .addModifiers(Modifier.STATIC)
                .addModifiers(Modifier.FINAL);
        return methodSpecBuilder.build();
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    private MethodSpec createMethodForRequiresPermission(TypeElement typeElement, Element e,
                                                         RequiresPermission.Before before,
                                                         RequiresPermission.OnDenied onDenied,
                                                         RequiresPermission requiresPermission)
            throws ClassNotFoundException {

        String methodName = e.getSimpleName().toString();

        // Params.
        String fullMethodName = e.toString();
        fullMethodName = fullMethodName.replace(methodName, "");
        fullMethodName = fullMethodName.replace("(", "").replace(")", "");

        StringTokenizer tokenizer = new StringTokenizer(fullMethodName, ",");
        int paramCount = tokenizer.countTokens();
        List<ParameterSpec> parameterSpecs = new ArrayList<>(paramCount);
        for (int i = 0; i < paramCount; i++) {
            String p = tokenizer.nextToken();
            if (PRIMITIVE_TYPES.containsKey(p)) {
                Type type = PRIMITIVE_TYPES.get(p);
                parameterSpecs.add(ParameterSpec.builder(type, "arg" + i, FINAL).build());
                continue;
            }
            String pkg = p.substring(0, p.lastIndexOf("."));
            String simpleName = p.replace(pkg + ".", "");
            Logger.debug("pkg: %s, simpleName: %s", pkg, simpleName);
            ClassName c = ClassName.get(pkg, simpleName);
            parameterSpecs.add(ParameterSpec.builder(c, "arg" + i, FINAL).build());
        }

        // Method name.
        methodName += requiresPermission.methodSubFix();

        // Permissions var.
        String[] perms = requiresPermission.value();
        StringBuilder permStatement = new StringBuilder("String permissions[] = new String[]{");
        for (String ignored : perms) {
            permStatement.append("$S").append(", ");
        }
        permStatement.append("}");
        Object[] permArgs = perms;

        int requestCode = REQUEST_CODE.incrementAndGet();

        String methodToCallInRunnable = e.getSimpleName().toString();
        StringBuilder onGrantPassingArgs = new StringBuilder("activity." + methodToCallInRunnable + "(");
        for (int i = 0; i < paramCount; i++) {
            ParameterSpec parameterSpec = parameterSpecs.get(i);
            if (i != paramCount - 1) onGrantPassingArgs.append(parameterSpec.name).append(", ");
            else onGrantPassingArgs.append(parameterSpec.name);
        }
        onGrantPassingArgs.append(");");

        // OnBefore.
        String onBeforeMethodName = null;
        if (before != null) onBeforeMethodName = before.value();
        String onBeforeCode = before == null ? ""
                : "activity." + onBeforeMethodName + "();\n";

        // OnDenied.
        String onDeniedMethodName = null;
        if (onDenied != null) onDeniedMethodName = onDenied.value();
        String onDeniedCode = onDenied == null ? ""
                :
                "Runnable r2 = new Runnable() {\n" +
                        "            @Override\n" +
                        "            public void run() {\n" +
                        "                activity." + onDeniedMethodName + "();\n" +
                        "            }\n" +
                        "        };\n" +
                        "ON_DENY_METHODS_MAP.put(code, r2);\n";

        MethodSpec.Builder methodSpecBuilder =
                MethodSpec.methodBuilder(methodName)
                        .addParameters(parameterSpecs)
                        .addParameter(ClassName.bestGuess(typeElement.getQualifiedName().toString()), "activity", FINAL)
                        .addCode(onBeforeCode)
                        .addStatement(permStatement.toString(), permArgs)
                        .addStatement("int code = $L", requestCode)

                        .addCode("Runnable r = new Runnable() {\n" +
                                "            @Override\n" +
                                "            public void run() {\n" +
                                "                " +
                                onGrantPassingArgs.toString() + "\n" +
                                "            }\n" +
                                "        };\n")
                        .addStatement("ON_GRANT_METHODS_MAP.put(code, r)")
                        .addCode(onDeniedCode)
                        .addStatement("android.support.v4.app.ActivityCompat.requestPermissions(activity, permissions, code)")
                        .addModifiers(Modifier.STATIC)
                        .addModifiers(Modifier.FINAL);
        return methodSpecBuilder.build();
    }

    private void checkModifiersIfNested(TypeElement type) {
        ElementKind enclosingKind = type.getEnclosingElement().getKind();
        if (enclosingKind.isClass() || enclosingKind.isInterface()) {
            if (type.getModifiers().contains(PRIVATE)) {
                mErrorReporter.abortWithError("@RuntimePermissions class must not be private", type);
            }
            if (!type.getModifiers().contains(STATIC)) {
                mErrorReporter.abortWithError("Nested @RuntimePermissions class must be static", type);
            }
        }
        // In principle type.getEnclosingElement() could be an ExecutableElement (for a class
        // declared inside a method), but since RoundEnvironment.getElementsAnnotatedWith doesn't
        // return such classes we won't see them here.
    }

    private boolean ancestorIs(TypeElement type, Class<? extends Annotation> clz) {
        while (true) {
            TypeMirror parentMirror = type.getSuperclass();
            if (parentMirror.getKind() == TypeKind.NONE) {
                return false;
            }
            TypeElement parentElement = (TypeElement) mTypeUtils.asElement(parentMirror);
            if (MoreElements.isAnnotationPresent(parentElement, clz)) {
                return true;
            }
            type = parentElement;
        }
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
}

