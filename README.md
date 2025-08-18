# MysteryDelivery

Automated delivery system for Mysterria server purchases. Handles various service types including items, subscriptions, permissions, and Discord roles.

## Features

- **Multiple Service Types Support**:
    - `ITEM` - Physical items delivered via commands
    - `KEY` - Dungeon keys and access tokens
    - `TOOL` - Special tools like compasses
    - `SUBSCRIPTION` - LuckPerms group subscriptions
    - `PERMISSION` - Temporary permissions via LuckPerms
    - `DISCORD_ROLE` - Discord role purchases (logged and announced)

- **Smart Delivery System**:
    - Queues deliveries for offline players (items/keys/tools)
    - Instant delivery for online players
    - Offline support for subscriptions/permissions via LuckPerms
    - Automatic retry mechanism for failed deliveries

- **Flexible Configuration**:
    - All delivery commands configured via metadata
    - No plugin changes needed for new services
    - Hot reload support (`/delivery reload`)

- **Multilingual Announcements**:
    - Purchase announcements in chat
    - Client-side translations (English/Ukrainian)
    - Global or purchaser-only announcements

## Dependencies

- Paper 1.21.4+
- CatWalk (API framework)
- LuckPerms (for permissions/subscriptions)

## Installation

1. Place the plugin JAR in your `plugins` folder
2. Ensure CatWalk and LuckPerms are installed
3. Start the server to generate configuration files
4. Configure your settings in `config.yml`

## Configuration

### config.yml
```yaml
announcements:
  enabled: true          # Enable purchase announcements
  global: true          # Announce to all players (false = purchaser only)

delivery:
  maxRetries: 3         # Max retry attempts for failed deliveries
  delayTicks: 60        # Delay before processing queued items on join
```

## API Endpoints

All endpoints require authentication (`requiresAuth = true`).

### Item/Key/Tool Delivery
```
POST /delivery/item
```
Delivers physical items, keys, or tools to players.

### Subscription Delivery
```
POST /delivery/subscription
```
Grants LuckPerms group subscriptions.

### Permission Delivery
```
POST /delivery/permission
```
Grants temporary permissions via LuckPerms.

### Discord Role Processing
```
POST /delivery/discord-role
```
Logs and announces Discord role purchases.

## Metadata Configuration

The plugin uses metadata from purchase requests to determine what commands to execute. This allows complete flexibility without plugin changes.

### Item/Key/Tool Metadata
```json
{
  "commands": [
    "give {player} diamond_sword 1",
    "noe token give {player} premium_key {quantity}"
  ]
}
```

### Subscription Metadata
```json
{
  "group": "vip",           // LuckPerms group name
  "duration": 30            // Duration in days
}
```

### Permission Metadata
```json
{
  "permissions": [          // List of permissions
    "essentials.fly",
    "essentials.tp"
  ],
  "duration": 7            // Duration in days
}
```

## Placeholders

Commands support the following placeholders:
- `{player}` - Player's username
- `{uuid}` - Player's UUID
- `{quantity}` - Purchase quantity
- `{service_name}` - Service name
- Any metadata field: `{field_name}`

## Commands

- `/delivery reload` - Reload configuration and translations
    - Permission: `mysterria.delivery.admin`

## Queue System

- Items, keys, and tools are queued if the player is offline
- Queue is saved to `queue.json` and persists across restarts
- Deliveries are processed automatically when player joins
- Failed deliveries are retried up to `maxRetries` times

## Adding New Services

To add a new service, simply configure it in your backend/database with appropriate metadata:

1. Set the service type (ITEM, SUBSCRIPTION, etc.)
2. Configure metadata with delivery commands or settings
3. Send purchase request to appropriate endpoint
4. Plugin automatically handles delivery based on metadata

No plugin changes or updates required!

## Examples

### Item Purchase with Commands
```json
{
  "serviceType": "ITEM",
  "metadata": {
    "commands": [
      "give {player} netherite_sword 1"
    ]
  }
}
```

### Subscription with Group
```json
{
  "serviceType": "SUBSCRIPTION",
  "metadata": {
    "group": "premium",
    "duration": 30
  }
}
```

### Multiple Commands for Kit
```json
{
  "serviceType": "ITEM",
  "metadata": {
    "commands": [
      "kit give {player} starter",
      "money give {player} 5000",
      "points give {player} 100"
    ]
  }
}
```

## Support

For issues or questions, contact the Mysterria development team.