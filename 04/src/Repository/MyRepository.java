package Repository;

import java.util.HashMap;

public class MyRepository<T> implements Repository<T>{
    private HashMap<Integer,T> hashMap =new HashMap<>();
    private int key=0;
    @Override
    public void save(T t) {
        hashMap.put(key,t);
        key++;
    }

    @Override
    public T getById(Integer id) {
        return hashMap.get(id);
    }

    public int insert(T t){
        int id=key;
        save(t);
        return id;
    }
    public void foreach(){
        for (T t : hashMap.values()){
            System.out.println(t);
        }
    }
}
