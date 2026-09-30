package Repository;

public interface Repository<T> {
    public void save(T t);
    public T getById(Integer id);
}
