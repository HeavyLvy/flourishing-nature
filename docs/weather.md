# Rain and snow

When rain or snow falls, farm animals head for cover; animals with thick coats carry on.

- **Who shelters** (`core/.../weather/WeatherTolerance`): cows, mooshrooms, sheep, pigs,
  chickens, horses, donkeys and rabbits. Wolves and foxes (and everything else) don't mind.
- **Only real weather counts.** It has to be raining or snowing *and* the animal's biome has to
  get precipitation (`Weather.precipitating`). Deserts, savannas and badlands report "raining"
  during a storm but never get wet, so animals there stay put. The same rule feeds the
  wellbeing shelter need.
- **What counts as cover:** any dry spot an animal can stand in with no sky above its head:
  under leaves, an overhang or cliff, a roof, or a cave mouth. `Weather.nearestShelter`
  searches rings out to 14 blocks, a few blocks up and down.
- **Behaviour** (`SeekShelterGoal`, priority 3): when it starts raining, an exposed animal walks
  to the nearest cover and stays there until the weather clears. While sheltered it holds its
  movement, so strolling and herd-following don't pull it back out. Panic and breeding still
  win. When the rain stops, the herd goals walk it back to its herd's home, which always stays
  under open sky.

## Tests

`WeatherGameTests` run in a rainy test environment so other tests stay dry. The test world is a
desert, so the shelter test first turns its pen into plains with `/fillbiome`.

- `cow_shelters_from_rain`: a cow walks under a leafy roof 10 blocks away (fails with the goal
  removed).
- `desert_cow_ignores_rain`: in a desert the shelter goal never runs.
- `wolf_ignores_rain`: wolves never get the goal.
