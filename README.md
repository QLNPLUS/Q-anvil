# Q Anvil

Forge 1.20.1 mod. The Q Anvil reuses the vanilla `AnvilMenu` calculation, but pays with player health and/or QShop currency instead of experience levels.

## Configuration

The common config is `config/qanvil-common.toml`:

- `cost_mode = "health"`: the default cost mode. Valid values are `health`, `currency`, or `both`.
- `currency_id = "coins"`: the default QShop currency id.
- `currency_per_level = 1.0`: currency units per vanilla anvil level.
- `health_per_level = 1.0`: Minecraft health points per vanilla anvil level.
- `keep_one_health = true`: prevent a payment from reducing the player below one health point.
- `max_input_stack_size = 64`: maximum stack size accepted by the Q Anvil left and right input slots. Set it from `1` to `999`; the default `64` preserves vanilla behavior.

QShop is optional. With `cost_mode = "health"`, Q Anvil works without QShop. If QShop is installed, the default currency is `coins` unless the config is changed.

## KubeJS

KubeJS integration is optional. The event is `QAnvilEvents.update` and runs on the server after the vanilla anvil result has been calculated.

```js
QAnvilEvents.update(event => {
  // event.left/event.input are the left slot; event.right/event.addition are the right slot.
  // event.originalOutput is the vanilla result. All three values are ItemStacks.
  // ItemStackJS.of(...) accepts strings, Item.of(...), and normal KubeJS item objects.
  if (event.input.id == 'minecraft:diamond_sword') {
    event.output = Item.of('minecraft:diamond_sword');
    event.currencyId = 'points';
    event.currencyCost = 25;
    event.healthCost = 2;
  }
});
```

Available event properties include `player`, `left`, `right`, `input`, `addition`, `originalOutput`, `output`, `vanillaLevelCost`, `currencyId`, `currencyCost`, and `healthCost`. `left`/`input` refer to the first slot, while `right`/`addition` refer to the second slot. Call `event.cancel()` to reject the operation. The configured `cost_mode` supplies the default costs. When KubeJS sets either cost property, KubeJS takes control of the cost selection; the other cost is cleared unless the script also sets it. Set a cost to `0` to disable it.

The Q Anvil is obtained in a vanilla smithing table using a Netherite Upgrade Smithing Template, a vanilla anvil, and a netherite ingot. It can also be obtained with `/give @s qanvil:q_anvil`. The recipe uses the vanilla `smithing_transform` type; no custom recipe type is registered.
