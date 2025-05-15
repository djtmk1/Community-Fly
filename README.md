# CommunityFly

**CommunityFly** is a Minecraft plugin that provides a flight time system allowing players to earn, donate, trade, and manage their flight time within the server. It integrates with Essentials, Vault economy, PlaceholderAPI, and supports MySQL or SQLite for data persistence.

---

## Features

- Flight time management per player with database persistence (MySQL/SQLite)
- Flight time consumption while flying with restrictions on worlds and height
- Flight time donation GUI with Vault economy integration
- Flight time trading between players
- Flight time bonuses on first join and daily login
- Flight disabled automatically when flight time runs out or player is inactive
- Configurable aesthetics: particles, flight meter action bar, low-time warnings
- PlaceholderAPI support to show flight time placeholders
- Admin commands to give, take, set, check flight time, reload config/database
- Integration with Essentials to avoid conflicts with Essentials flight system

---

## Requirements

- Minecraft server running Spigot or Paper (1.16+ recommended)
- [Vault](https://dev.bukkit.org/projects/vault) (for economy features)
- An economy plugin supported by Vault (e.g., EssentialsX, iConomy)
- [Essentials](https://essentialsx.net/) (optional but recommended)
- [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) (optional for placeholders)

---

## Installation

1. Download the latest CommunityFly plugin `.jar` file from the releases.
2. Place the `.jar` file into your server's `plugins` directory.
3. Start your server to generate the default config.
4. Configure `config.yml` located in `plugins/CommunityFly` as needed.
5. (Optional) Configure your database settings in `config.yml` (MySQL or SQLite).
6. Restart the server.

---

## Configuration

The main config file allows customizing:

```yaml
database:
  type: sqlite # or mysql
  mysql:
    host: localhost
    port: 3306
    database: communityfly
    user: root
    password: ""

restrictions:
  disabled-worlds:
    - "world_nether"
    - "world_the_end"
  max-height: 256.0

inactivity:
  timeout: 300000 # milliseconds (5 minutes)
  penalty: 1.0
  disable-flight: true

bonuses:
  daily: 300.0
  first-join: 600.0

aesthetics:
  low-time-warning:
    threshold: 30.0
  particles:
    enabled: true
    type: CLOUD
  flight-meter:
    enabled: true
