package kr.fallen.elemental;

import io.wispforest.accessories.api.AccessoriesCapability;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import java.util.*;

public class ElementalEyes implements ModInitializer {
    public static final String MODID="elemental_eyes";
    public static final Map<Item,Element> EYES=new HashMap<>();
    public static final Map<Item,Element> TESTERS=new HashMap<>();
    public static Item FIRE_EYE,WATER_EYE,ELECTRIC_EYE,WIND_EYE,EARTH_EYE;
    public static Item FIRE_TESTER,WATER_TESTER,ELECTRIC_TESTER,WIND_TESTER,EARTH_TESTER;
    private static Item reg(String id,Item item){return Registry.register(Registries.ITEM,Identifier.of(MODID,id),item);}
    private static Item eye(String id,Element e){Item i=reg(id,new Item(new Item.Settings().maxCount(1)));EYES.put(i,e);return i;}
    private static Item tester(String id,Element e){Item i=reg(id,new Item(new Item.Settings().maxCount(1)));TESTERS.put(i,e);return i;}
    @Override public void onInitialize(){
        FIRE_EYE=eye("fire_eye",Element.FIRE); WATER_EYE=eye("water_eye",Element.WATER);
        ELECTRIC_EYE=eye("electric_eye",Element.ELECTRIC); WIND_EYE=eye("wind_eye",Element.WIND); EARTH_EYE=eye("earth_eye",Element.EARTH);
        FIRE_TESTER=tester("fire_tester",Element.FIRE); WATER_TESTER=tester("water_tester",Element.WATER);
        ELECTRIC_TESTER=tester("electric_tester",Element.ELECTRIC); WIND_TESTER=tester("wind_tester",Element.WIND); EARTH_TESTER=tester("earth_tester",Element.EARTH);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries->{EYES.keySet().forEach(entries::add);TESTERS.keySet().forEach(entries::add);});
    }
    public static Element attackingElement(LivingEntity attacker){
        Element forced=TESTERS.get(attacker.getMainHandStack().getItem());
        if(forced==null) forced=TESTERS.get(attacker.getOffHandStack().getItem());
        if(forced!=null)return forced;
        try{
            var cap=AccessoriesCapability.get(attacker);
            if(cap!=null) for(var pair:cap.getAllEquipped()){
                Element e=EYES.get(pair.stack().getItem()); if(e!=null)return e;
            }
        }catch(Throwable ignored){}
        return null;
    }
}
