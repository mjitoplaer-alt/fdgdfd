package ru.holyworld.keysnatcher;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;

public final class HolyWorldKeySnatcher implements ClientModInitializer {
    private int lastEntityId = Integer.MIN_VALUE;
    private String lastSnapshot = "";

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onEndTick);
    }

    private void onEndTick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            reset();
            return;
        }

        // Diagnostic only: inspect whatever entity the crosshair currently hits.
        // No automated interaction/clicks are sent in this build.
        if (!(client.crosshairTarget instanceof EntityHitResult hit)) {
            reset();
            return;
        }

        Entity entity = hit.getEntity();
        String snapshot = describe(entity);

        // Send one chat report when aiming at a new entity, or when its visible
        // name/equipment changes while the crosshair remains on it.
        if (entity.getId() != lastEntityId || !snapshot.equals(lastSnapshot)) {
            client.player.sendMessage(
                    Text.literal("[KeySnatcher DEBUG] " + snapshot),
                    false
            );
            lastEntityId = entity.getId();
            lastSnapshot = snapshot;
        }
    }

    private String describe(Entity entity) {
        String customName = entity.getCustomName() == null
                ? "<none>"
                : clean(entity.getCustomName().getString());

        ItemStack main = ItemStack.EMPTY;
        ItemStack off = ItemStack.EMPTY;
        if (entity instanceof net.minecraft.entity.LivingEntity living) {
            main = living.getMainHandStack();
            off = living.getOffHandStack();
        }

        return "entityType=" + Registries.ENTITY_TYPE.getId(entity.getType())
                + " | class=" + entity.getClass().getName()
                + " | name=" + clean(entity.getName().getString())
                + " | display=" + clean(entity.getDisplayName().getString())
                + " | customName=" + customName
                + " | mainHand=" + describeStack(main)
                + " | offHand=" + describeStack(off);
    }

    private String describeStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "<empty>";
        }
        return Registries.ITEM.getId(stack.getItem())
                + " [" + clean(stack.getName().getString())
                + "] x" + stack.getCount();
    }

    private String clean(String value) {
        return value.replace('\n', ' ').replace('\r', ' ');
    }

    private void reset() {
        lastEntityId = Integer.MIN_VALUE;
        lastSnapshot = "";
    }
}
