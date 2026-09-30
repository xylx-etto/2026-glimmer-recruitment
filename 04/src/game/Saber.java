package game;

public class Saber implements Character {
    private String name;
    private String skill;

    public Saber() {
        this.name = "阿尔托莉雅";
        this.skill = "Excalibur";
    }

    @Override
    public void attack() {
        System.out.println("[剑士] "+name+" 使用 "+skill+" 发动攻击！");
    }
    // TODO: 实现 attack 方法，打印格式为："[剑士] 阿尔托莉雅 使用 Excalibur 发动攻击！"
}