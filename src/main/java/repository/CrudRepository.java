package repository;

import java.io.File;

public interface CrudRepository<T> {

    //todos los metodos que vayan a acceder a los datos
    public T create();
    public T read();
    public T update(String content);
    public T delete(String content);

    //borra...




}
