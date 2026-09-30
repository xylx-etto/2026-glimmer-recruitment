package game;

public class Archer implements Character {
    private String name;
    private String skill;

    public Archer() {
        this.name = "卫宫";
        this.skill = "Unlimited Blade Works";
    }

    @Override
    public void attack() {
        System.out.println("[弓兵] "+name+" 使用 "+skill+" 发动攻击！");
    }
    // TODO: 实现 attack 方法
    // attack 格式："[弓兵] 卫宫 使用 Unlimited Blade Works 发动攻击！"
}
