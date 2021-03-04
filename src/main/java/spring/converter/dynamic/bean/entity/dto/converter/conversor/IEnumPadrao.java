package spring.converter.dynamic.bean.entity.dto.converter.conversor;

public interface IEnumPadrao<T> {

    T getKey();

    String getValue();

    default String getName() {
        return ((Enum) this).name();
    }

}
