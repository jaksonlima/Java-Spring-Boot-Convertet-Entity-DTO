package spring.converter.dynamic.bean.entity.dto.converter.conversor;

import java.io.Serializable;

public abstract class Entity<T extends Number> implements IPreIdentifier<T>, Serializable  {

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{id: " + getId() + "}";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        final var other = (Entity) obj;
        if (this.getId() != null && other.getId() == null) {
            return false;
        }

        if (other.getId() != null && this.getId() == null) {
            return false;
        }

        if (this.getId() == null && other.getId() == null) {
            return false;
        }

        return this.getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        var hash = 5;
        hash = 71 * hash + (this.getId() != null ? this.getId().hashCode() : 0);
        return hash;
    }

}
