QAnvilEvents.update(event => {
  if (event.left.id == 'minecraft:diamond_sword' && event.right.id == 'minecraft:netherite_ingot') {
    event.output = Item.of('minecraft:netherite_sword');
    event.currencyId = 'coins';
    event.currencyCost = 20;
    event.healthCost = 4;
  }
});
