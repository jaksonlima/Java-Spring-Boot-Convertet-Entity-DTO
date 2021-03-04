package spring.converter.dynamic.bean.entity.dto.converter.conversor;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * @apiNote MDEntity -> Model Entity
 * @apiNote MDDTO -> Model DTO
 */
public class DynamicConverter<MDDTO extends DTO<?>, MDEntity extends Entity<?>> {

    private Class<MDDTO> dtoClass;

    private Class<MDEntity> entityClass;

    public DynamicConverter() {
        var actualTypeArguments = ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments();
        this.dtoClass = (Class<MDDTO>) actualTypeArguments[0];
        this.entityClass = (Class<MDEntity>) actualTypeArguments[1];
    }

    public MDEntity converterDTOParaEntity(MDDTO MDDTO) {
        return converterDTOParaEntity(MDDTO, this.entityClass);
    }

    public List<MDEntity> converterDTOParaEntity(List<MDDTO> MDDTOS) {
        return converterDTOParaEntity(MDDTOS, this.entityClass);
    }

    public MDDTO converterEntityParaDTO(MDEntity MDEntity) {
        return converterEntityParaDTO(MDEntity, this.dtoClass);
    }

    public List<MDDTO> converterEntityParaDTO(List<MDEntity> MDEntities) {
        return converterEntityParaDTO(MDEntities, this.dtoClass);
    }

    public static <D extends DTO<?>, E extends Entity> E converterDTOParaEntity(D dto, Class<E> clazzEntity) {
        return (E) converter(dto, clazzEntity);
    }

    public static <D extends DTO<?>, E extends Entity> List<E> converterDTOParaEntity(List<D> dtos, Class<E> clazzEntity) {
        return dtos.stream().map(dto -> (E) converter(dto, clazzEntity)).collect(Collectors.toList());
    }

    public static <D extends DTO<?>, E extends Entity> D converterEntityParaDTO(E entity, Class<D> clazzDto) {
        return (D) converter(entity, clazzDto);
    }

    public static <D extends DTO<?>, E extends Entity> List<D> converterEntityParaDTO(List<E> entitys, Class<D> clazzDto) {
        return entitys.stream().map(entity -> (D) converter(entity, clazzDto)).collect(Collectors.toList());
    }

    private static Object converter(Object objectOrigem, Class classDestino) {
        return converter(objectOrigem, classDestino, null);
    }

    private static Object converter(Object objectOrigem, Class classDestino, List<Field> fieldsEquals) {
        try {
            if (Objects.isNull(objectOrigem) || Objects.isNull(classDestino)) {
                return null;
            }

            if (fieldsEquals != null || !fieldsEquals.isEmpty()) {
                fieldsEquals = onlyEqualsFields(objectOrigem.getClass(), classDestino);
            }

            if (Objects.isNull(fieldsEquals)) {
                return null;
            }

            Map<String, Object> toMapPopulateObjectDesino = new HashMap<>();

            fieldsEquals.stream().forEach(field -> {
                final var pathField = onlyPathFieldOrPathMappedFieldDTO(field);
                final var valueFieldOrigem = onlyValueField(objectOrigem, pathField);
                final var valueFieldTypeDTO = onlyValueFieldTypeDTO(valueFieldOrigem, field);

                var value = Objects.nonNull(valueFieldTypeDTO) ? valueFieldTypeDTO : valueFieldOrigem;

                if (value != null) {
                    value = parseEnum(value, field);

                    toMapPopulateObjectDesino.put(field.getName(), value);
                }
            });

            return populad(toMapPopulateObjectDesino, classDestino);
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    private static Object onlyValueFieldTypeDTO(Object valueFieldOrigem, Field field) {
        try {
            if (valueFieldOrigem != null) {
                final var clazzValue = valueFieldOrigem.getClass();
                final var clazzField = field.getType();

                if (!clazzValue.equals(clazzField)) {
                    final var fieldsToConvert = onlyFieldsFromOnlyField(field, clazzField);

                    if (IPreIdentifier.class.isAssignableFrom(clazzValue) && IPreIdentifier.class.isAssignableFrom(clazzField)) {
                        return converter(valueFieldOrigem, clazzField, fieldsToConvert);
                    }

                    if (Collection.class.isAssignableFrom(clazzValue) && Collection.class.isAssignableFrom(clazzField)) {
                        final var classFieldTypeCollection = parseClassParameterizedTypeField(field);
                        if (IPreIdentifier.class.isAssignableFrom(classFieldTypeCollection)) {
                            return ((Collection) valueFieldOrigem).stream()
                                    .map(value -> converter(value, classFieldTypeCollection, fieldsToConvert))
                                    .collect(Collectors.toList());
                        }
                    }
                }
            }
            return null;
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    private static List<Field> onlyEqualsFields(Class classOrigem, Class classDestino) {
        final List<Field> fieldsOrigem = new ArrayList();
        onlyAddIfNotExistsFieldModifier(fieldsOrigem, Arrays.asList(classOrigem.getDeclaredFields()));
        onlyAddIfNotExistsFieldModifier(fieldsOrigem, Arrays.asList(classOrigem.getSuperclass().getDeclaredFields()));

        final List<Field> fieldsDestino = new ArrayList();
        onlyAddIfNotExistsFieldModifier(fieldsDestino, Arrays.asList(classDestino.getDeclaredFields()));
        onlyAddIfNotExistsFieldModifier(fieldsDestino, Arrays.asList(classDestino.getSuperclass().getDeclaredFields()));

        final var fieldsDestIsMappedFieldDTO = fieldsDestino.stream()
                .filter(fd -> fd.isAnnotationPresent(UsingMappedOnlyFields.class))
                .collect(Collectors.toList());

        final var fieldsTransientFieldDTO = fieldsDestino.stream()
                .filter(fd -> fd.isAnnotationPresent(UsingTransientFieldDTO.class))
                .collect(Collectors.toList());

        final var fieldsEqualsOrigiDest = fieldsOrigem.stream()
                .flatMap(fo -> fieldsDestino.stream().filter(fd -> fd.getName().equalsIgnoreCase(fo.getName())))
                .collect(Collectors.toList());

        final List<Field> fields = new ArrayList<>();

        if (!CollectionUtils.isEmpty(fieldsDestIsMappedFieldDTO)) {
            fields.addAll(fieldsDestIsMappedFieldDTO);
        }

        if (!CollectionUtils.isEmpty(fieldsEqualsOrigiDest)) {
            fields.addAll(fieldsEqualsOrigiDest);
        }

        if (!CollectionUtils.isEmpty(fieldsTransientFieldDTO)) {
            fields.removeAll(fieldsTransientFieldDTO);
        }

        if (spring.converter.dynamic.bean.entity.dto.converter.conversor.DTO.class.isAssignableFrom(classDestino)) {
            final var fieldsNotDeclared = onlyFieldsNotDeclared(classOrigem, fieldsDestino, fieldsEqualsOrigiDest);
            onlyAllIfNotNull(fields, fieldsNotDeclared);
        }

        return fields;
    }

    private static List<Field> onlyFieldsNotDeclared(Class classOrigem, List<Field> fieldsDestino, List<Field> fieldsOrigem) {
        final List<Field> fields = new ArrayList();

        fieldsDestino.removeAll(fieldsOrigem);
        if (!CollectionUtils.isEmpty(fieldsDestino)) {
            for (var field : fieldsDestino) {
                final var function = parseGetterMethod(field.getName(), classOrigem);
                if (function != null) {
                    fields.add(field);
                }
            }
        }

        return fields;
    }

    private static List<Field> onlyFieldsFromOnlyField(Field field, Class<?> clazzValue) {
        if (field.isAnnotationPresent(UsingOnlyField.class)) {
            if (Collection.class.isAssignableFrom(clazzValue) && Collection.class.isAssignableFrom(field.getType())) {
                clazzValue = parseClassParameterizedTypeField(field);
            }

            final var valuesOnlyField = Arrays.asList(field.getAnnotation(UsingOnlyField.class).value());

            final List<Field> fieldsOrigem = new ArrayList();
            onlyAddIfNotExistsFieldModifier(fieldsOrigem, Arrays.asList(clazzValue.getDeclaredFields()));
            onlyAddIfNotExistsFieldModifier(fieldsOrigem, Arrays.asList(clazzValue.getSuperclass().getDeclaredFields()));

            return valuesOnlyField.stream().flatMap(s -> fieldsOrigem.stream().filter(fd -> fd.getName().equalsIgnoreCase(s))).collect(Collectors.toList());
        }

        return null;
    }

    private static String onlyPathFieldOrPathMappedFieldDTO(final Field field) {
        if (field.isAnnotationPresent(UsingMappedOnlyFields.class)) {
            final var value = field.getAnnotation(UsingMappedOnlyFields.class).value();
            return value != null ? value : field.getName();
        } else {
            return field.getName();
        }
    }

    private static Object onlyValueField(Object objectOrigem, String pathField) {
        if (Objects.nonNull(objectOrigem) && Objects.nonNull(pathField)) {
            return BeanInvokeDynamic.getFieldValue(objectOrigem, pathField);
        }
        return null;
    }

    private static Object parseEnum(Object value, Field field) {
        if (field.getType().isAssignableFrom(PadraoEnumDTO.class)) {
            if (value instanceof Enum) {
                value = new PadraoEnumDTO((IEnumPadrao<?>) value);
            }
        }

        if (!value.getClass().equals(field.getType())) {
            if (field.getType().isEnum() && value instanceof PadraoEnumDTO) {
                final var padraoDTO = (PadraoEnumDTO) value;

                value = Stream.of(field.getType().getEnumConstants())
                        .map(valueEnum -> ((Enum) valueEnum))
                        .filter(valueEnum ->
                                valueEnum.name().equalsIgnoreCase((String) padraoDTO.getKey()) ||
                                        valueEnum.name().equalsIgnoreCase((String) padraoDTO.getName())
                        )
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Não encontrado " + padraoDTO.getKey()));
            }
        }
        return value;
    }

    private static Object populad(Map propertiesToMap, Class classDestino) {
        try {
            var instanceObjectDestino = classDestino.newInstance();
            BeanUtilsBean.getInstance().populate(instanceObjectDestino, propertiesToMap);
            return instanceObjectDestino;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    private static void onlyAddIfNotExistsFieldModifier(Collection<Field> collectionsFields, List<Field> fields) {
        fields.forEach(field -> {
            if (!collectionsFields.contains(field)) {
                if (!Modifier.isFinal(field.getModifiers()) && !Modifier.isStatic(field.getModifiers())) {
                    collectionsFields.add(field);
                }
            }
        });
    }

    public static Class parseClassParameterizedTypeField(Field field) {
        return (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
    }

    public static Method parseGetterMethod(String fieldName, Class<?> javaBeanClass) {
        return Stream.of(javaBeanClass.getDeclaredMethods())
                .filter(method -> onlyGetterMethod(method, fieldName))
                .findFirst()
                .orElse(null);
    }

    private static boolean onlyGetterMethod(Method method, String name) {
        return method.getParameterCount() == 0
                && !Modifier.isStatic(method.getModifiers())
                && (method.getName().equalsIgnoreCase("get" + name) || method.getName().equalsIgnoreCase("is" + name));
    }

    public static <T> void onlyAllIfNotNull(Collection<T> collection, Collection<T> values) {
        if (values != null || !values.isEmpty()) {
            for (T value : values) {
                onlyIfNotNull(collection, value);
            }
        }
    }

    public static <T> boolean onlyIfNotNull(Collection<T> collection, T value) {
        if (value != null) {
            return collection.add(value);
        }
        return false;
    }

}
