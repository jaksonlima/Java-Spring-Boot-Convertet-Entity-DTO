package spring.converter.dynamic.bean.entity.dto.converter.conversor;

import java.io.Serializable;

public interface IPreIdentifier<T extends Number> extends Serializable {

    T getId();

}
