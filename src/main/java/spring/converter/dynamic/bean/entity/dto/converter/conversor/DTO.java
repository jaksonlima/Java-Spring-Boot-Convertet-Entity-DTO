package spring.converter.dynamic.bean.entity.dto.converter.conversor;

import lombok.Data;

import java.io.Serializable;

@Data
public abstract class DTO<T extends Number> implements IPreIdentifier<T>, Serializable  {
}
