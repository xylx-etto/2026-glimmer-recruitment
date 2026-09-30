package game;

    public class Caster implements Character {
        private String name;
        private String skill;

        public Caster() {
            this.name = "美狄亚";
            this.skill = "Rho Aias";
        }

        @Override
        public void attack() {
            System.out.println("[法师] "+name+" 使用 "+skill+" 发动攻击！");
        }
        // TODO: 实现 attack 方法
        // attack 格式："[法师] 美狄亚 使用 Rho Aias 发动攻击！"
    }