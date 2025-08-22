package org.babyfish.jimmer.apt.immutable.meta;

import com.squareup.javapoet.ClassName;
import org.babyfish.jimmer.Formula;
import org.babyfish.jimmer.apt.Context;
import org.babyfish.jimmer.apt.MetaException;
import org.babyfish.jimmer.dto.compiler.spi.BaseType;
import org.babyfish.jimmer.lang.Ref;
import org.babyfish.jimmer.sql.*;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ImmutableType implements BaseType {

    public static final String PROP_EXPRESSION_SUFFIX = "PropExpression";

    private static final String FORMULA_CLASS_NAME = Formula.class.getName();

    private final TypeElement typeElement;

    private final boolean isEntity;

    private final boolean isMappedSuperClass;

    private final boolean isEmbeddable;

    private final String packageName;

    private final String name;

    private final String qualifiedName;

    private final Set<Modifier> modifiers;

    private final ImmutableType primarySuperType;

    private final Set<ImmutableType> superTypes;

    private final Map<String, ImmutableProp> declaredProps;

    private final Map<String, ImmutableProp> redefinedProps;

    private Map<String, ImmutableProp> props;

    private Map<String, String> idPropNameMap;

    private List<ImmutableProp> propsOrderById;

    private ImmutableProp idProp;

    private ImmutableProp versionProp;

    private ImmutableProp logicalDeletedProp;

    private final ClassName className;

    private final ClassName draftClassName;

    private final ClassName producerClassName;

    private final ClassName implementorClassName;

    private final ClassName implClassName;

    private final ClassName draftImplClassName;

    private final ClassName builderClassName;

    private final ClassName tableClassName;

    private final ClassName tableExClassName;

    private final ClassName remoteTableClassName;

    private final ClassName fetcherClassName;

    private final ClassName propsClassName;

    private final ClassName propExpressionClassName;

    private final ClassName dynamicClassName;

    private final Map<ClassName, String> validationMessageMap;

    private final boolean acrossMicroServices;

    private final String microServiceName;
    
    public ImmutableType(
            Context context,
            TypeElement typeElement
    ) {
        this.typeElement = typeElement;
        Class<?> annotationType = context.getImmutableAnnotationType(typeElement);
        isEntity = annotationType == Entity.class;
        acrossMicroServices = annotationType == MappedSuperclass.class &&
                typeElement.getAnnotation(MappedSuperclass.class).acrossMicroServices();
        microServiceName = isEntity ?
                typeElement.getAnnotation(Entity.class).microServiceName() :
                annotationType == MappedSuperclass.class ?
                    typeElement.getAnnotation(MappedSuperclass.class).microServiceName() :
                    "";
        if (acrossMicroServices && !microServiceName.isEmpty()) {
            throw new MetaException(
                    typeElement,
                    "the `acrossMicroServices` of its annotation \"@" +
                            MappedSuperclass.class.getName() +
                            "\" is true so that `microServiceName` cannot be specified"
            );
        }
        isMappedSuperClass = annotationType == MappedSuperclass.class;
        isEmbeddable = annotationType == Embeddable.class;

        packageName = ((PackageElement)typeElement.getEnclosingElement()).getQualifiedName().toString();
        name = typeElement.getSimpleName().toString();
        qualifiedName = typeElement.getQualifiedName().toString();
        modifiers = typeElement.getModifiers();

        ImmutableType primarySuperType = null;
        Set<ImmutableType> superTypes = new LinkedHashSet<>();
        for (TypeMirror itf : typeElement.getInterfaces()) {
            if (context.isImmutable(itf)) {
                ImmutableType superType = context.getImmutableType(itf);
                superTypes.add(superType);
                if (!superType.isMappedSuperClass) {
                    if (primarySuperType == null) {
                        primarySuperType = superType;
                    } else {
                        throw new MetaException(
                                typeElement,
                                "there can be at most one primary superclass not decorated by @MappedSuperclass"
                        );
                    }
                }
            }
        }
        if (!superTypes.isEmpty()) {
            if (isEntity || isMappedSuperClass) {
                for (ImmutableType superType : superTypes) {
                    if (superType.isEntity) {
                        if (isMappedSuperClass) {
                            throw new MetaException(
                                    typeElement,
                                    "mapped super class cannot inherit entity type"
                            );
                        }
                    } else if (!superType.isMappedSuperClass) {
                        throw new MetaException(
                                typeElement,
                                "its super type \"" +
                                        superType +
                                        "\"" +
                                        (isEntity ?
                                                "is neither entity nor mapped super class" :
                                                "is not mapped super class"
                                        )
                        );
                    }
                }
            } else if (isEmbeddable) {
                throw new MetaException(
                        typeElement,
                        "embedded type does not support inheritance"
                );
            } else {
                if (superTypes.size() > 1) {
                    throw new MetaException(
                            typeElement,
                            "simple immutable type does not support multiple inheritance"
                    );
                }
                for (ImmutableType superType : superTypes) {
                    if (superType.isEntity || superType.isMappedSuperClass || superType.isEmbeddable) {
                        throw new MetaException(
                                typeElement,
                                "simple immutable type can only inherit simple immutable type"
                        );
                    }
                }
            }
        }

        for (ImmutableType superType : superTypes) {
            if (!superType.isAcrossMicroServices() && !superType.microServiceName.equals(microServiceName)) {
                throw new MetaException(
                        typeElement,
                        "its micro service name is \"" +
                                microServiceName +
                                "\" but the micro service name of its super type \"" +
                                superType.getQualifiedName() +
                                "\" is \"" +
                                superType.microServiceName +
                                "\""
                );
            }
        }
        this.primarySuperType = primarySuperType;
        this.superTypes = superTypes;
        for (ImmutableType superType : superTypes) {
            if (!superType.isMappedSuperClass) {
                primarySuperType = superType;
                break;
            }
        }

        Map<String, ImmutableProp> superPropMap = new LinkedHashMap<>();
        for (ImmutableType superType : superTypes) {
            for (ImmutableProp prop : superType.getProps().values()) {
                ImmutableProp conflictProp = superPropMap.put(prop.getName(), prop);
                if (conflictProp != null) {
                    if (conflictProp.getGetterName().equals(prop.getGetterName())) {
                        throw new MetaException(
                                typeElement,
                                "There are two super properties with the same name: \"" +
                                        conflictProp +
                                        "\" and \"" +
                                        prop +
                                        "\", but their java getter name are different"
                        );
                    }
                    if (!conflictProp.getReturnType().equals(prop.getReturnType())) {
                        throw new MetaException(
                                typeElement,
                                "There are two super properties with the same name: \"" +
                                        conflictProp +
                                        "\" and \"" +
                                        prop +
                                        "\", but their return type are different"
                        );
                    }
                }
            }
        }

        Map<String, ImmutableProp> redefiningSuperProps;
        if (primarySuperType == null) {
            redefiningSuperProps = superPropMap;
        } else {
            redefiningSuperProps = new LinkedHashMap<>(superPropMap);
            redefiningSuperProps.keySet().removeAll(primarySuperType.getProps().keySet());
        }
        int propIdSequence = primarySuperType != null ? primarySuperType.getProps().size() : 0;
        Map<String, ImmutableProp> redefinedPropMap = new LinkedHashMap<>(
                (redefiningSuperProps.size() * 4 + 2) / 3
        );
        Map<String, ImmutableProp> declaredPropMap =
                primarySuperType != null ?
                        new LinkedHashMap<>(primarySuperType.getProps()) :
                        new LinkedHashMap<>();

        List<ExecutableElement> executableElements = new ArrayList<>();
        for (ImmutableProp redefiningSuperProp : redefiningSuperProps.values()) {
            executableElements.add(redefiningSuperProp.toElement());
            ImmutableProp prop = new ImmutableProp(
                    context,
                    this,
                    redefiningSuperProp.toElement(),
                    propIdSequence++
            );
            redefinedPropMap.put(prop.getName(), prop);
        }
        for (int i = 0; i < 2; i++) {
            for (ExecutableElement executableElement : ElementFilter.methodsIn(typeElement.getEnclosedElements())) {
                if (((executableElement.getAnnotation(Id.class)) != null) == (i == 0)) {
                    if (superPropMap.containsKey(executableElement.getSimpleName().toString())) {
                        throw new MetaException(
                                executableElement,
                                "it overrides property of super type, this is not allowed"
                        );
                    }
                    if (executableElement.getAnnotation(Trait.class) != null) {
                        continue;
                    }
                    executableElements.add(executableElement);
                    ImmutableProp prop = new ImmutableProp(
                            context,
                            this,
                            executableElement,
                            propIdSequence++
                    );
                    ImmutableProp conflictProp = declaredPropMap.put(prop.getName(), prop);
                    if (conflictProp != null) {
                        throw new MetaException(
                                prop.toElement(),
                                "Conflict java methods: " +
                                        prop.getGetterName() +
                                        " and " +
                                        conflictProp.getGetterName()
                        );
                    }
                }
            }
        }

        for (ExecutableElement executableElement : executableElements) {
            if (executableElement.isDefault()) {
                for (AnnotationMirror am : executableElement.getAnnotationMirrors()) {
                    String qualifiedName = ((TypeElement)am.getAnnotationType().asElement()).getQualifiedName().toString();
                    if (qualifiedName.startsWith("org.babyfish.jimmer.") && !qualifiedName.equals(FORMULA_CLASS_NAME)) {
                        throw new MetaException(
                                executableElement,
                                "it " +
                                        "is default method so that it cannot be decorated by " +
                                        "any jimmer annotations except @" +
                                        FORMULA_CLASS_NAME
                        );
                    }
                }
            }
        }
        for (ExecutableElement executableElement : executableElements) {
            Formula formula = executableElement.getAnnotation(Formula.class);
            if (isEmbeddable && formula != null && !formula.sql().isEmpty()) {
                throw new MetaException(
                        executableElement,
                        "The sql based formula property cannot be declared in embeddable type"
                );
            }
            if (executableElement.isDefault()) {
                if (formula != null) {
                    if (!formula.sql().isEmpty()) {
                        throw new MetaException(
                                executableElement,
                                "it is non-abstract and decorated by @" +
                                        Formula.class.getName() +
                                        ", non-abstract modifier means simple calculation property based on " +
                                        "java expression so that the `sql` of that annotation cannot be specified"
                        );
                    }
                    if (formula.dependencies().length == 0) {
                        throw new MetaException(
                                executableElement,
                                "it is non-abstract and decorated by @" +
                                        Formula.class.getName() +
                                        ", non-abstract modifier means simple calculation property based on " +
                                        "java expression so that the `dependencies` of that annotation must be specified"
                        );
                    }
                }
            } else if (executableElement.getAnnotation(Id.class) == null) {
                if (formula != null) {
                    if (formula.sql().isEmpty()) {
                        throw new MetaException(
                                executableElement,
                                "it is abstract and decorated by @" +
                                        Formula.class.getName() +
                                        ", abstract modifier means simple calculation property based on " +
                                        "SQL expression so that the `sql` of that annotation must be specified"
                        );
                    }
                    if (formula.dependencies().length != 0) {
                        throw new MetaException(
                                executableElement,
                                "it is abstract and decorated by @" +
                                        Formula.class.getName() +
                                        ", abstract modifier means simple calculation property based on " +
                                        "SQL expression so that the `dependencies` of that annotation cannot be specified"
                        );
                    }
                }
            }
        }

        this.declaredProps = Collections.unmodifiableMap(declaredPropMap);
        this.redefinedProps = Collections.unmodifiableMap(redefinedPropMap);

        List<ImmutableProp> idProps = declaredProps
                .values()
                .stream()
                .filter(it -> it.getAnnotation(Id.class) != null)
                .collect(Collectors.toList());
        List<ImmutableProp> versionProps = declaredProps
                .values()
                .stream()
                .filter(it -> it.getAnnotation(Version.class) != null)
                .collect(Collectors.toList());
        List<ImmutableProp> logicalDeletedProps = declaredProps
                .values()
                .stream()
                .filter(it -> it.getAnnotation(LogicalDeleted.class) != null)
                .collect(Collectors.toList());
        for (ImmutableType superType : superTypes) {
            if (superType.getIdProp() != null && !idProps.isEmpty()) {
                throw new MetaException(
                        typeElement,
                        idProps.get(0) +
                                "\" cannot be decorated by `@" +
                                Id.class.getName() +
                                "` because id has been declared in super type"
                );
            }
            if (superType.getVersionProp() != null && !versionProps.isEmpty()) {
                throw new MetaException(
                        typeElement,
                        versionProps.get(0) +
                                "\" cannot be decorated by `@" +
                                Version.class.getName() +
                                "` because version has been declared in super type"
                );
            }
            if (superType.getLogicalDeletedProp() != null && !logicalDeletedProps.isEmpty()) {
                throw new MetaException(
                        typeElement,
                        logicalDeletedProps.get(0) +
                                "\" cannot be decorated by `@" +
                                LogicalDeleted.class.getName() +
                                "` because version has been declared in super type"
                );
            }
            if (idProp == null) {
                idProp = superType.idProp;
            }
            if (versionProp == null) {
                versionProp = superType.versionProp;
            }
            if (logicalDeletedProp == null) {
                logicalDeletedProp = superType.logicalDeletedProp;
            }
        }
        if (!isEntity && !isMappedSuperClass) {
            if (!idProps.isEmpty()) {
                throw new MetaException(
                        typeElement,
                        idProps.get(0) +
                                "\" cannot be decorated by `@" +
                                Id.class.getName() +
                                "` because current type is not entity"
                );
            }
            if (!versionProps.isEmpty()) {
                throw new MetaException(
                        typeElement,
                                versionProps.get(0) +
                                "\" cannot be decorated by `@" +
                                Version.class.getName() +
                                "` because current type is not entity"
                );
            }
            if (!logicalDeletedProps.isEmpty()) {
                throw new MetaException(
                        typeElement,
                                logicalDeletedProps.get(0) +
                                "\" cannot be decorated by `@" +
                                LogicalDeleted.class.getName() +
                                "` because current type is not entity"
                );
            }
        } else {
            if (idProps.size() > 1) {
                throw new MetaException(
                        typeElement,
                        "multiple id properties are not supported, " +
                                "but both \"" +
                                idProps.get(0) +
                                "\" and \"" +
                                idProps.get(1) +
                                "\" is decorated by `@" +
                                LogicalDeleted.class.getName() +
                                "`"
                );
            }
            if (versionProps.size() > 1) {
                throw new MetaException(
                        typeElement,
                        "multiple version properties are not supported, " +
                                "but both \"" +
                                versionProps.get(0) +
                                "\" and \"" +
                                versionProps.get(1) +
                                "\" is decorated by `@" +
                                Version.class.getName() +
                                "`"
                );
            }
            if (logicalDeletedProps.size() > 1) {
                throw new MetaException(
                        typeElement,
                        "multiple logical deleted properties are not supported, " +
                                "but both \"" +
                                logicalDeletedProps.get(0) +
                                "\" and \"" +
                                logicalDeletedProps.get(1) +
                                "\" is decorated by `@" +
                                LogicalDeleted.class.getName() +
                                "`"
                );
            }
            if (idProp == null) {
                if (isEntity && idProps.isEmpty()) {
                    throw new MetaException(
                            typeElement,
                            "entity type must have an id property"
                    );
                }
                if (!idProps.isEmpty()) {
                    idProp = idProps.get(0);
                }
            }
            if (idProp != null && idProp.isAssociation(true)) {
                throw new MetaException(
                        typeElement,
                        "association cannot be id property"
                );
            }
            if (versionProp == null && !versionProps.isEmpty()) {
                versionProp = versionProps.get(0);
                if (versionProp.isAssociation(false)) {
                    throw new MetaException(
                            typeElement,
                            "association cannot be version property"
                    );
                }
            }
            if (logicalDeletedProp == null && !logicalDeletedProps.isEmpty()) {
                logicalDeletedProp = logicalDeletedProps.get(0);
                if (logicalDeletedProp.isAssociation(false)) {
                    throw new MetaException(
                            typeElement,
                            "it contains illegal property \"" +
                                    logicalDeletedProps +
                                    "\", association cannot be logical deleted property"
                    );
                }
            }
        }

        className = toClassName(null);
        draftClassName = toClassName(name -> name + "Draft");
        producerClassName = toClassName(name -> name + "Draft", "Producer");
        implementorClassName = toClassName(name -> name + "Draft", "Producer", "Implementor");
        implClassName = toClassName(name -> name + "Draft", "Producer", "Impl");
        draftImplClassName = toClassName(name -> name + "Draft", "Producer", "DraftImpl");
        builderClassName = toClassName(name -> name + "Draft", "Builder");
        tableClassName = toClassName(name -> name + "Table");
        tableExClassName = toClassName(name -> name + "TableEx");
        remoteTableClassName = toClassName(name -> name + "Table", "Remote");
        fetcherClassName = toClassName(name -> name + "Fetcher");
        propsClassName = toClassName(name -> name + "Props");
        propExpressionClassName = toClassName(name -> name + PROP_EXPRESSION_SUFFIX);
        dynamicClassName = toClassName(name -> "Dynamic" + name);
        validationMessageMap = ValidationMessages.parseMessageMap(typeElement);
    }

    public TypeElement getTypeElement() {
        return typeElement;
    }

    public boolean isEntity() {
        return isEntity;
    }

    public boolean isMappedSuperClass() {
        return isMappedSuperClass;
    }

    public boolean isEmbeddable() {
        return isEmbeddable;
    }

    public boolean isAcrossMicroServices() {
        return acrossMicroServices;
    }

    @Override
    public String getPackageName() {
        return packageName;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getQualifiedName() {
        return qualifiedName;
    }

    public Set<Modifier> getModifiers() {
        return modifiers;
    }

    public Set<ImmutableType> getSuperTypes() {
        return superTypes;
    }

    public ImmutableType getPrimarySuperType() {
        return primarySuperType;
    }

    public Map<String, ImmutableProp> getDeclaredProps() {
        return declaredProps;
    }

    public Map<String, ImmutableProp> getRedefinedProps() {
        return redefinedProps;
    }

    public Map<String, ImmutableProp> getProps() {
        Map<String, ImmutableProp> props = this.props;
        if (props == null) {
            if (superTypes.isEmpty()) {
                props = declaredProps;
            } else {
                props = new LinkedHashMap<>();
                for (ImmutableType superType : superTypes) {
                    for (ImmutableProp prop : superType.getProps().values()) {
                        if (prop.getAnnotation(Id.class) != null) {
                            props.put(prop.getName(), prop);
                        }
                    }
                }
                for (ImmutableProp prop : redefinedProps.values()) {
                    if (prop.getAnnotation(Id.class) != null) {
                        props.put(prop.getName(), prop);
                    }
                }
                for (ImmutableProp prop : declaredProps.values()) {
                    if (prop.getAnnotation(Id.class) != null) {
                        props.put(prop.getName(), prop);
                    }
                }
                for (ImmutableType superType : superTypes) {
                    for (ImmutableProp prop : superType.getProps().values()) {
                        if (prop.getAnnotation(Id.class) == null) {
                            props.put(prop.getName(), prop);
                        }
                    }
                }
                for (ImmutableProp prop : redefinedProps.values()) {
                    if (prop.getAnnotation(Id.class) == null) {
                        props.put(prop.getName(), prop);
                    }
                }
                for (ImmutableProp prop : declaredProps.values()) {
                    if (prop.getAnnotation(Id.class) == null) {
                        props.put(prop.getName(), prop);
                    }
                }
            }
            this.props = Collections.unmodifiableMap(props);
        }
        return props;
    }

    public String getIdPropName(String prop) {
        return getIdPropNameMap().get(prop);
    }

    private Map<String, String> getIdPropNameMap() {
        Map<String, String> map = this.idPropNameMap;
        if (map == null) {
            map = new HashMap<>();
            for (ImmutableProp prop : props.values()) {
                ImmutableProp baseProp = prop.getIdViewBaseProp();
                if (baseProp != null) {
                    map.put(baseProp.getName(), prop.getName());
                }
            }
            for (ImmutableProp prop : props.values()) {
                if (prop.isReverse()) {
                    continue;
                }
                if (prop.getAnnotation(OneToOne.class) == null && prop.getAnnotation(ManyToOne.class) == null) {
                    continue;
                }
                if (map.containsKey(prop.getName())) {
                    continue;
                }
                String expectedPropName = prop.getName() + "Id";
                ImmutableProp expectedProp = getProps().get(expectedPropName);
                if (expectedProp != null) {
                    throw new MetaException(
                            expectedProp.toElement(),
                            "It looks like @IdView of association \"" +
                                    prop +
                                    "\", please add the @IdView annotation"
                    );
                }
                map.put(prop.getName(), expectedPropName);
            }
            this.idPropNameMap = map;
        }
        return map;
    }

    public List<ImmutableProp> getPropsOrderById() {
        List<ImmutableProp> list = propsOrderById;
        if (list == null) {
            this.propsOrderById = list = getProps()
                    .values()
                    .stream()
                    .sorted(Comparator.comparing(ImmutableProp::getId))
                    .collect(Collectors.toList());
        }
        return list;
    }

    public ImmutableProp getIdProp() {
        return idProp;
    }

    public ImmutableProp getVersionProp() {
        return versionProp;
    }

    public ImmutableProp getLogicalDeletedProp() {
        return logicalDeletedProp;
    }

    public ClassName getClassName() {
        return className;
    }

    public ClassName getDraftClassName() {
        return draftClassName;
    }

    public ClassName getProducerClassName() {
        return producerClassName;
    }

    public ClassName getImplementorClassName() {
        return implementorClassName;
    }

    public ClassName getImplClassName() {
        return implClassName;
    }

    public ClassName getDraftImplClassName() {
        return draftImplClassName;
    }

    public ClassName getBuilderClassName() {
        return builderClassName;
    }

    public ClassName getTableClassName() {
        return tableClassName;
    }

    public ClassName getTableExClassName() {
        return tableExClassName;
    }

    public ClassName getRemoteTableClassName() {
        return remoteTableClassName;
    }

    public ClassName getFetcherClassName() {
        return fetcherClassName;
    }

    public ClassName getPropsClassName() {
        return propsClassName;
    }

    public ClassName getPropExpressionClassName() {
        return propExpressionClassName;
    }

    public ClassName getDynamicClassName() {
        return dynamicClassName;
    }

    private ClassName toClassName(
            Function<String, String> transform,
            String ... moreSimpleNames
    ) {
        return ClassName.get(
                packageName,
                transform != null ? transform.apply(name) : name,
                moreSimpleNames
        );
    }

    public Map<ClassName, String> getValidationMessageMap() {
        return validationMessageMap;
    }

    public String getMicroServiceName() {
        return microServiceName;
    }

    @Override
    public String toString() {
        return typeElement.getQualifiedName().toString();
    }
}
