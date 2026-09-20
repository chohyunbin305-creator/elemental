package kr.fallen.elemental;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import java.lang.reflect.Method;
import java.util.*;

public class ElementalEyes implements ModInitializer {
    public static final String MODID="elemental_eyes";
    public static final Map<Item,Element> TESTERS=new HashMap<>();
    public static Item FIRE_TESTER,WATER_TESTER,ELECTRIC_TESTER,WIND_TESTER,EARTH_TESTER;

    private static Item reg(String id,Item item){return Registry.register(Registries.ITEM,Identifier.of(MODID,id),item);}
    private static Item tester(String id,Element e){Item i=reg(id,new Item(new Item.Settings().maxCount(1)));TESTERS.put(i,e);return i;}

    @Override public void onInitialize(){
        FIRE_TESTER=tester("fire_tester",Element.FIRE);
        WATER_TESTER=tester("water_tester",Element.WATER);
        ELECTRIC_TESTER=tester("electric_tester",Element.ELECTRIC);
        WIND_TESTER=tester("wind_tester",Element.WIND);
        EARTH_TESTER=tester("earth_tester",Element.EARTH);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(e->TESTERS.keySet().forEach(e::add));
    }

    public static Element attackingElement(LivingEntity attacker){
        Element forced=TESTERS.get(attacker.getMainHandStack().getItem());
        if(forced==null) forced=TESTERS.get(attacker.getOffHandStack().getItem());
        if(forced!=null) return forced;
        if(!(attacker instanceof PlayerEntity player)) return null;
        try{
            Class<?> originClass=Class.forName("io.github.apace100.origins.origin.Origin");
            Method get=originClass.getMethod("get",PlayerEntity.class);
            Map<?,?> map=(Map<?,?>)get.invoke(null,player);
            for(Object origin:map.values()){
                Object id=originClass.getMethod("getId").invoke(origin);
                String s=String.valueOf(id);
                if(s.equals(MODID+":fire")) return Element.FIRE;
                if(s.equals(MODID+":water")) return Element.WATER;
                if(s.equals(MODID+":electric")) return Element.ELECTRIC;
                if(s.equals(MODID+":wind")) return Element.WIND;
                if(s.equals(MODID+":earth")) return Element.EARTH;
            }
        }catch(Throwable ignored){}
        return null;
    }
}
