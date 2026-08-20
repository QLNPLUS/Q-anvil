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

KubeJS integration is optional. Put the script in `kubejs/server_scripts/`, for example
`kubejs/server_scripts/qanvil.js`. The event runs on the server every time the Q Anvil
recalculates its result: when either input changes, or when the name is edited.

Register the event with:

```js
QAnvilEvents.update(event => {
  // Your rule goes here.
});
```

### Input and output

The event exposes both descriptive names and vanilla-style aliases:

- `event.left` and `event.input`: the left input slot.
- `event.right` and `event.addition`: the right input slot.
- `event.originalOutput`: the result calculated by the vanilla anvil before this event.
- `event.output`: the final result. It can be assigned an `Item.of(...)` stack or another KubeJS item stack.
- `event.player`: the `ServerPlayer` using the Q Anvil.
- `event.vanillaLevelCost`: the original vanilla anvil level cost.

The input stacks include normal KubeJS properties such as `id`, `count`, and `nbt`.
Always check both inputs when the recipe needs a specific right-side material:

```js
QAnvilEvents.update(event => {
  const left = event.left;
  const right = event.right;

  if (left.id != 'minecraft:diamond_sword') return;
  if (right.id != 'minecraft:netherite_ingot') return;

  event.output = Item.of('minecraft:netherite_sword');
});
```

`event.originalOutput` can be used to preserve the vanilla result and only change
the payment:

```js
QAnvilEvents.update(event => {
  if (event.originalOutput.empty) return;
  if (event.left.id != 'minecraft:diamond_sword') return;

  event.output = event.originalOutput.copy();
  event.currencyId = 'points';
  event.currencyCost = 25;
  event.healthCost = 2;
});
```

### Payment properties

The explicit payment properties are:

- `event.currencyId`: the QShop currency id for this operation.
- `event.currencyCost`: the exact currency amount to charge.
- `event.healthCost`: the exact player health amount to charge.

Property assignment and setter calls are both supported:

```js
event.currencyId = 'points';
event.currencyCost = 25;
event.healthCost = 2;
```

The equivalent method form is:

```js
event.setCurrencyId('points');
event.setCurrencyCost(25);
event.setHealthCost(2);
```

Setting only one explicit cost disables the other cost. To charge both, set both
properties. A value of `0` disables that part of the payment:

```js
event.currencyCost = 25;
event.healthCost = 0;
```

### Forge-style compatibility methods

The event also supports the common Forge `AnvilUpdateEvent` method names:

```js
event.setOutput(output);
event.setCost(5);
event.setMaterialCost(1);
```

Equivalent property assignments are supported:

```js
event.output = output;
event.cost = 5;
event.materialCost = 1;
```

`setCost(5)` is not an experience-level payment. It means a direct payment of `5`
in the configured `cost_mode`:

- `health`: 5 health points.
- `currency`: 5 units of the selected currency.
- `both`: 5 health points and 5 currency units.

Explicit `currencyCost` and `healthCost` values take priority over `setCost`.
Therefore, this always charges 25 currency and 2 health, not 5:

```js
event.currencyCost = 25;
event.healthCost = 2;
event.setCost(5); // Does not override the two explicit costs above.
```

`materialCost` controls how many items are consumed from the right input slot after
the output is taken:

- `1`: consume one right-side item.
- `3`: consume three right-side items.
- `0`: consume the entire right-side stack.
- A value greater than the right-side count also consumes the entire right-side stack.

If `materialCost` is not set, the normal vanilla anvil material-consumption result
is used. `materialCost` is independent of currency and health payment.

### Complete examples

Custom output with one right-side material consumed:

```js
QAnvilEvents.update(event => {
  const left = event.left;
  const right = event.right;

  if (left.id != 'minecraft:diamond_sword') return;
  if (right.id != 'minecraft:netherite_ingot') return;

  const output = Item.of('minecraft:netherite_sword');
  event.setOutput(output);
  event.currencyId = 'coins';
  event.currencyCost = 25;
  event.healthCost = 2;
  event.materialCost = 1;
});
```

Legacy Forge-style syntax with the configured payment mode:

```js
QAnvilEvents.update(event => {
  if (event.left.id != 'minecraft:diamond_sword') return;

  const output = event.left.copy();
  event.setOutput(output);
  event.setCost(5);
  event.setMaterialCost(1);
});
```

Reject an operation completely:

```js
QAnvilEvents.update(event => {
  if (event.right.id == 'minecraft:bedrock') {
    event.cancel();
  }
});
```

The configured `cost_mode` supplies the default payment when the script does not
set a payment property. KubeJS rules run with higher priority than the config, so
the script can select health, currency, or both for an individual operation.

The Q Anvil is obtained in a vanilla smithing table using a Netherite Upgrade Smithing Template, a vanilla anvil, and a netherite ingot. It can also be obtained with `/give @s qanvil:q_anvil`. The recipe uses the vanilla `smithing_transform` type; no custom recipe type is registered.
