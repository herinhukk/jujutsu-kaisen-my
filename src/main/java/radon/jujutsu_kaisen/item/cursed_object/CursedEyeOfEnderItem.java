package radon.jujutsu_kaisen.item.cursed_object;

import radon.jujutsu_kaisen.cursed_technique.CursedTechnique;

import radon.jujutsu_kaisen.data.sorcerer.SorcererGrade;
import radon.jujutsu_kaisen.item.CursedObjectItem;

public class CursedEyeOfEnderItem extends CursedObjectItem {
    public CursedEyeOfEnderItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public SorcererGrade getGrade() {
        return SorcererGrade.GRADE_2;
    }
}
