package kr.fallen.elemental.client;

import kr.fallen.elemental.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class CharacterClient {
    private static CharacterStatePayload state=new CharacterStatePayload(0,0,false,false,0,0);
    private static KeyBinding primary,secondary;
    private static boolean wasPrimary,wasSecondary;
    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(CharacterStatePayload.ID,(p,c)->c.client().execute(()->{
            state=p;
            if(c.client().player!=null) {
                c.client().player.removeCommandTag("elemental_android_client");
                c.client().player.removeCommandTag("elemental_outlaw_client");
                if(p.kind()==1)c.client().player.addCommandTag("elemental_android_client");
                if(p.kind()==2)c.client().player.addCommandTag("elemental_outlaw_client");
            }
        }));
        ClientPlayNetworking.registerGlobalReceiver(BeamPayload.ID,(p,c)->c.client().execute(()->renderBeam(p)));
        ClientTickEvents.END_CLIENT_TICK.register(CharacterClient::tick);
        HudRenderCallback.EVENT.register((context,tick)->hud(context));
    }
    private static KeyBinding binding(String field) {
        try {return (KeyBinding)Class.forName("io.github.apace100.origins.OriginsClient").getField(field).get(null);}
        catch(ReflectiveOperationException e){return null;}
    }
    private static void tick(MinecraftClient c) {
        if(c.player==null || c.world==null) {state=new CharacterStatePayload(0,0,false,false,0,0);wasPrimary=false;wasSecondary=false;return;}
        if(primary==null)primary=binding("primaryActiveKeyBinding");
        if(secondary==null)secondary=binding("secondaryActiveKeyBinding");
        boolean a=c.currentScreen==null && primary!=null && primary.isPressed();
        boolean b=c.currentScreen==null && secondary!=null && secondary.isPressed();
        if((state.kind()!=0 || wasPrimary || wasSecondary) && (a!=wasPrimary || b!=wasSecondary || c.player.age%5==0))
            ClientPlayNetworking.send(new ControlPayload(a,b));
        wasPrimary=a;wasSecondary=b;
        if(state.kind()==1 && c.player.isTouchingWater()) {
            c.player.setSwimming(false);c.player.setSprinting(false);
            Vec3d v=c.player.getVelocity(); c.player.setVelocity(v.x*.8,Math.min(v.y,.035),v.z*.8);
        }
        if(state.firing()) {
            c.player.input.movementForward=0;c.player.input.movementSideways=0;
            c.player.input.jumping=false;c.player.setVelocity(Vec3d.ZERO);
        }
    }
    private static void renderBeam(BeamPayload p) {
        MinecraftClient c=MinecraftClient.getInstance();if(c.world==null)return;
        Vec3d a=new Vec3d(p.x(),p.y(),p.z()),b=new Vec3d(p.ex(),p.ey(),p.ez());
        Vec3d dir=b.subtract(a);double length=dir.length();if(length<.001)return;
        dir=dir.normalize();
        Vector3f color=p.style()==0?new Vector3f(.15f,1f,.38f):p.style()==1?new Vector3f(.15f,.55f,1f):new Vector3f(.4f,.23f,.10f);
        DustParticleEffect effect=new DustParticleEffect(color,p.style()==1?1.7f:p.style()==2?.65f:1.0f);
        Vec3d side=dir.crossProduct(new Vec3d(0,1,0));
        if(side.lengthSquared()<.001)side=new Vec3d(1,0,0);else side=side.normalize();
        Vec3d up=side.crossProduct(dir).normalize();
        for(double d=0;d<Math.min(length,128);d+=p.style()==2?.35:.3) {
            Vec3d pos=a.add(dir.multiply(d));
            int count=p.style()==1?5:1;
            for(int i=0;i<count;i++) {
                Vec3d v=pos;
                if(i>0) {double angle=i*Math.PI/2;v=v.add(side.multiply(Math.cos(angle)*.7)).add(up.multiply(Math.sin(angle)*.7));}
                var particle=c.particleManager.addParticle(effect,v.x,v.y,v.z,0,0,0);
                if(particle!=null)particle.setMaxAge(p.style()==0?4:3);
            }
        }
    }
    private static void text(DrawContext g,String s,int x,int y,int color) {
        g.drawTextWithShadow(MinecraftClient.getInstance().textRenderer,s,x,y,color);
    }
    private static void hud(DrawContext g) {
        MinecraftClient c=MinecraftClient.getInstance();
        if(c.player==null || c.world==null || state.kind()==0 || c.options.hudHidden)return;
        int w=g.getScaledWindowWidth(),h=g.getScaledWindowHeight(),x=w/2+22,y=h-83;
        text(g,state.kind()==1?"정밀분석 "+state.gauge()+"%":"무법자",x,y,0x77DDFF);
        text(g,(state.kind()==1?"디스플레이":"잽싼 움직임")+": "+cooldown(state.mainCooldown()),x,y+11,0xBBDDDD);
        text(g,(state.kind()==1?"스매시 런처":"올가미")+": "+cooldown(state.subCooldown()),x,y+22,0xBBDDDD);
        if(state.kind()!=1 || !state.display() || !c.options.getPerspective().isFirstPerson())return;
        g.fill(6,6,w-6,8,0xAA55CCFF);g.fill(6,h-8,w-6,h-6,0xAA55CCFF);
        g.fill(6,8,8,h-8,0x7755CCFF);g.fill(w-8,8,w-6,h-8,0x7755CCFF);
        g.fill(8,8,170,65,0x55102132);
        text(g,"ANDROID / 정밀분석",15,15,0x66DDFF);
        text(g,String.format("XYZ %.1f / %.1f / %.1f",c.player.getX(),c.player.getY(),c.player.getZ()),15,28,0xAAEFFF);
        String biome=c.world.getBiome(c.player.getBlockPos()).getKey().map(k->k.getValue().getPath()).orElse("unknown");
        text(g,"BIOME "+biome,15,40,0xAAEFFF);
        long time=(c.world.getTimeOfDay()+6000)%24000;
        text(g,String.format("TIME %02d:%02d | %s",time/1000,(time%1000)*60/1000,c.player.getHorizontalFacing().asString()),15,52,0xAAEFFF);
        g.fill(w-125,15,w-15,44,0x66102132);
        text(g,"ANALYSIS "+state.gauge()+"%",w-119,20,0x77FFCC);
        g.fill(w-119,34,w-19,38,0xAA254253);g.fill(w-119,34,w-119+state.gauge(),38,0xFF55DDAA);
        var camera=c.gameRenderer.getCamera(); Vec3d eye=camera.getPos();
        Quaternionf inverse=new Quaternionf(camera.getRotation()).conjugate();
        double focal=h/(2*Math.tan(Math.toRadians(c.options.getFov().getValue())/2));
        int count=0;
        for(LivingEntity e:c.world.getEntitiesByClass(LivingEntity.class,c.player.getBoundingBox().expand(40),
            e->e!=c.player && e.isAlive() && !e.isSpectator() && c.player.canSee(e))) {
            Vec3d delta=e.getBoundingBox().getCenter().subtract(eye);
            Vector3f v=new Vector3f((float)delta.x,(float)delta.y,(float)delta.z).rotate(inverse);
            if(v.z>=-.3)continue;
            int sx=(int)(w/2+v.x*focal/-v.z),sy=(int)(h/2-v.y*focal/-v.z);
            if(sx<12 || sx>w-12 || sy<12 || sy>h-12)continue;
            int r=(int)Math.clamp(e.getHeight()*focal/-v.z*.3,6,45);
            int color=0xCC66FFDD;
            g.fill(sx-r,sy-r,sx-r+7,sy-r+1,color);g.fill(sx-r,sy-r,sx-r+1,sy-r+7,color);
            g.fill(sx+r-7,sy-r,sx+r,sy-r+1,color);g.fill(sx+r-1,sy-r,sx+r,sy-r+7,color);
            g.fill(sx-r,sy+r-1,sx-r+7,sy+r,color);g.fill(sx+r-7,sy+r-1,sx+r,sy+r,color);
            text(g,String.format("%.0fm",delta.length()),sx-r,sy+r+3,0x66FFDD);count++;
        }
        text(g,"TARGETS "+count,15,76,0x66FFDD);
    }
    private static String cooldown(int ticks) {return ticks==0?"준비":String.format("%.1fs",ticks/20.0);}
}
