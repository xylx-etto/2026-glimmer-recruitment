package animal;

public class Penguin extends Animal{
    String name;
    int id;
    public Penguin(String b, int i) {
        super(b,i);
//        name=b;
//        id=i;
    }
    @Override
    public void introduction(){
        super.introduction();
        System.out.println("大家好！我是" + id + "号企鹅" + name + "。");
    }
}