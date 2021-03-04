package spring.converter.dynamic.bean.entity.dto.converter.conversor;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PadraoEnumDTO {

    private Object key;

    private Object name;

    private String value;

    public PadraoEnumDTO(IEnumPadrao<?> enumPadrao) {
        this.key = enumPadrao.getName();
        this.name = enumPadrao.getKey();
        this.value = enumPadrao.getValue();
    }
}
