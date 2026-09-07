# Compat layer — патч-нота для будущей доработки

Файл `src/main/java/ru/khozain/inhphaton/compat/PacketAbstraction.java`
сейчас работает в безопасном no-op-режиме: все вызовы `sendSpawn /
sendEntityDestroy / sendEntityTeleport` логируются, но не отправляют
настоящие пакеты.

## Почему так

- Paper 26.2 (Minecraft 1.21.x) внутренние классы переименовываются
  между минорными билдами, а NMS-прямой код выходит за рамки задачи.
- Вся версионно-зависимая логика вынесена в один класс, как требует
  п.11 ТЗ.

## Что нужно сделать для полной поддержки

1. На целевом сервере 26.2 найти имена:
   - `net.minecraft.network.protocol.game.ClientboundSpawnEntityPacket`
   - `net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket`
   - `net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket`
2. Реализовать `ru.khozain.inhphaton.compat.nms.EntityDestroySender`
   и `…EntityTeleportSender` с точными сигнатурами.
3. Снять no-op в `PacketAbstraction` — добавить реальный `try/catch`
   который вызывает `EntityDestroySender.destroy(viewer, phantomId)`,
   `EntityTeleportSender.teleport(viewer, phantomId, loc)`.

## Альтернатива

Использовать Bukkit-API `Player#spawnEntity` для создания backing entity,
а затем прятать/показывать через `Player#showEntity / hideEntity`.
Это работает для «осязаемых» фантомов (которые могут иметь AI),
но не для чисто-визуальных иллюзий.