# Game Fast Booster — Server setup (WireGuard)

One rented VPS per region to start: UAE, Singapore, Germany.
A $5–10/month VPS per region is enough for early testing.

## 1. Install WireGuard on the server (Ubuntu/Debian)

```bash
sudo apt update && sudo apt install -y wireguard
sudo sysctl -w net.ipv4.ip_forward=1
echo "net.ipv4.ip_forward=1" | sudo tee -a /etc/sysctl.conf
```

## 2. Generate the server keypair (ON THE SERVER ONLY)

```bash
wg genkey | sudo tee /etc/wireguard/privatekey | wg pubkey | sudo tee /etc/wireguard/publickey
```

The **public key** goes into the app's `ServerConfig.kt`.
The **private key never leaves the server and never goes in the app.**

## 3. Server config — /etc/wireguard/wg0.conf (example)

```ini
[Interface]
Address = 10.8.0.1/24
ListenPort = 51820
PrivateKey = <SERVER_PRIVATE_KEY>
PostUp = iptables -A FORWARD -i wg0 -j ACCEPT; iptables -t nat -A POSTROUTING -o eth0 -j MASQUERADE
PostDown = iptables -D FORWARD -i wg0 -j ACCEPT; iptables -t nat -D POSTROUTING -o eth0 -j MASQUERADE

# One [Peer] block per user device. Each user gets their OWN keypair.
[Peer]
PublicKey = <USER_DEVICE_PUBLIC_KEY>
AllowedIPs = 10.8.0.2/32
```

Start it: `sudo wg-quick up wg0` and open UDP port 51820 in the VPS firewall.

## 4. Per-user provisioning (required before Connect works)

The app currently throws a clear error until a device keypair exists,
because sharing one private key across all users would be a security
disaster. Before launch, add a small backend endpoint that:

1. Receives a new user's WireGuard public key (generated on their phone).
2. Assigns them a free address (10.8.0.x).
3. Appends a `[Peer]` block on the right region's server and reloads it.
4. Returns the assigned address to the app.

Until that endpoint exists, test with ONE manually added peer (your own phone).

## 5. What this server does NOT do

- It does not log user traffic (no-logs policy — do not add logging).
- It does not touch the PUBG game client or bypass anti-cheat.
- It does not guarantee 20 ms. It gives the phone the shortest route
  the network allows, and the app shows the player's own measured ms.
