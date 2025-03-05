# FlopBot 🤖

<div align="Left">
    <strong>An open-source Discord bot for the Flopcoin community</strong>
</div>

## 📝 Features

### 🔗 Links & Information
- `/twitter` - Get the official Twitter account
- `/reddit` - Get the Reddit community link
- `/website` - Visit the official website
- `/explorer` - Access the blockchain explorer
- `/pools` - View mining pools information
- `/inscriptions` - Information about Flopcoin inscriptions
- `/github` - Access the GitHub repository
- `/whitepaper` - Read the Flopcoin whitepaper

### 💰 Wallet Management
- `/balance` - Check your wallet balance
- `/withdraw` - Withdraw FLOP from your wallet

### 🎰 Casino Games
- `/blackjack` - Play blackjack with FLOP
- `/coinflip` - Flip a coin to win FLOP
- `/slots` - Play the slot machine with various themes
- `/crash` - Play the crash game

### 📊 Staking System
- `/stake new` - Create a new FLOP stake
- `/stake list` - View your active stakes
- `/stake claim` - Claim daily stake rewards
- `/stake end` - End an active stake
- `/stake stats` - View server-wide staking statistics
- `/stake help` - Learn how to stake your coins

### 🎁 Faucet & Donations
- `/faucet` - Claim free FLOP from the faucet
- `/donate` - Donate FLOP to the faucet

### 🛠️ Utility Commands
- `/coinstats` - View Flopcoin statistics and price
- `/network` - Check network information
- `/roles` - Manage Discord roles
- `/help` - View all available commands

## 🚀 Setup

### Prerequisites
- Java 17 or higher
- MongoDB database
- Discord Bot Token
- Flopcoin Core Node (running with RPC access)
- LiveCoinWatch API key

### Configuration
Create a `.env` file in the root directory with the following variables:
```env
# Discord Bot Configuration
TOKEN=your_discord_bot_token
GUILD_ID=your_discord_server_id
COIN_EMOJI=your_coin_emoji

# Database Configuration
DATABASE=your_mongodb_uri

# Flopcoin RPC Configuration
RPC_USER=your_rpc_username
RPC_PASSWORD=your_rpc_password
RPC_URL=your_rpc_url

# API Keys
LIVECOINWATCH_API_KEY=your_api_key

# Faucet Configuration
FAUCET_ADDRESS=your_faucet_wallet_address
```

### Installation
1. Clone the repository:
```bash
git clone https://github.com/TechnoVisionDev/FlopBot.git
cd FlopBot
```

2. Build the project:
```bash
./gradlew build
```

3. Run the bot:
```bash
java -jar build/libs/FlopBot-1.0.0.jar
```

## 💻 Development

The bot is built using the following technologies:
- JDA (Java Discord API)
- MongoDB for data persistence
- OkHttp for API requests
- Flopcoin Core RPC for blockchain interaction

### Project Structure
```
src/main/java/flopbot/
├── commands/         # Command implementations
├── handlers/         # Event and functionality handlers
├── data/            # Database and data models
├── util/            # Utility classes
└── FlopBot.java     # Main bot class
```

### Adding New Commands
1. Create a new command class in the appropriate category package
2. Extend the `Command` class
3. Implement the `execute` method
4. Register the command in `CommandRegistry.java`

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request. For major changes, please open an issue first to discuss what you would like to change.

1. Fork the repository
2. Create a new branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 💬 Support

For support, join our [Discord server](https://discord.gg/flopcoin) and ask in the appropriate channels.

## ⚡ Quick Links
- [Flopcoin Website](https://flopcoin.net)
- [Block Explorer](https://explorer.flopcoin.net)
- [GitHub Organization](https://github.com/Flopcoin)
