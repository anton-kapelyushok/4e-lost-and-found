# Systemd Service Setup

This guide shows how to set up the application as a systemd service so it:
- ✅ Starts automatically on server boot
- ✅ Restarts automatically on failure
- ✅ Can be managed with `systemctl` commands

## Setup Instructions

### 1. Edit the Service File

First, customize `fe-lnf.service` with your username:

```bash
cd ~/fe-lnf-deploy

# Replace YOUR_USERNAME with your actual username (e.g., anton)
sed -i "s|YOUR_USERNAME|$USER|g" fe-lnf.service

# Verify the paths are correct
cat fe-lnf.service
```

### 2. Install the Service

```bash
# Copy service file to systemd directory
sudo cp fe-lnf.service /etc/systemd/system/

# Reload systemd to recognize the new service
sudo systemctl daemon-reload

# Enable the service to start on boot
sudo systemctl enable fe-lnf.service
```

### 3. Start the Service

```bash
# Start the service now
sudo systemctl start fe-lnf

# Check status
sudo systemctl status fe-lnf
```

Expected output:
```
● fe-lnf.service - Lost and Found Application
     Loaded: loaded (/etc/systemd/system/fe-lnf.service; enabled; vendor preset: enabled)
     Active: active (exited) since Mon 2024-03-24 12:00:00 UTC; 5s ago
    Process: 12345 ExecStart=/usr/bin/docker-compose up -d (code=exited, status=0/SUCCESS)
   Main PID: 12345 (code=exited, status=0/SUCCESS)
```

## Managing the Service

### Basic Commands

```bash
# Start the application
sudo systemctl start fe-lnf

# Stop the application
sudo systemctl stop fe-lnf

# Restart the application
sudo systemctl restart fe-lnf

# Check status
sudo systemctl status fe-lnf

# View logs
sudo journalctl -u fe-lnf -f

# View last 100 lines
sudo journalctl -u fe-lnf -n 100
```

### Enable/Disable Auto-Start

```bash
# Enable auto-start on boot (already done in setup)
sudo systemctl enable fe-lnf

# Disable auto-start on boot
sudo systemctl disable fe-lnf

# Check if enabled
sudo systemctl is-enabled fe-lnf
```

## Testing Auto-Restart

Test that the service restarts automatically when containers fail:

```bash
# Simulate a failure by stopping a container
docker stop fe-lnf-app

# Wait 10 seconds (RestartSec setting)
# Then check if systemd restarted it
docker ps | grep fe-lnf

# Check systemd logs
sudo journalctl -u fe-lnf -n 50
```

## Testing Boot Auto-Start

Test that the service starts on server boot:

```bash
# Reboot the server
sudo reboot

# After reboot, check if service is running
sudo systemctl status fe-lnf
docker-compose ps
```

## Updating the Application

When you want to deploy a new version:

```bash
cd ~/fe-lnf-deploy

# Pull new image
docker-compose pull app

# Restart via systemd
sudo systemctl restart fe-lnf

# Or let systemd handle it:
# Stop the container and systemd will restart it with new image
docker stop fe-lnf-app
```

## Modifying the Service

If you need to change the service configuration:

```bash
# 1. Edit the service file
sudo nano /etc/systemd/system/fe-lnf.service

# 2. Reload systemd
sudo systemctl daemon-reload

# 3. Restart the service
sudo systemctl restart fe-lnf
```

## Troubleshooting

### Service won't start

```bash
# Check systemd logs
sudo journalctl -u fe-lnf -n 100

# Check if Docker is running
sudo systemctl status docker

# Check docker-compose
cd ~/fe-lnf-deploy
docker-compose config
```

### Service starts but containers fail

```bash
# Systemd starts successfully, but check Docker logs
cd ~/fe-lnf-deploy
docker-compose logs -f

# The systemd service just runs docker-compose
# Container issues are in Docker logs, not systemd logs
```

### Service doesn't restart on failure

```bash
# Check service status
sudo systemctl status fe-lnf

# If it shows "failed", restart it
sudo systemctl restart fe-lnf

# Check restart configuration
systemctl cat fe-lnf | grep Restart
```

## Uninstalling the Service

If you need to remove the systemd service:

```bash
# Stop and disable the service
sudo systemctl stop fe-lnf
sudo systemctl disable fe-lnf

# Remove the service file
sudo rm /etc/systemd/system/fe-lnf.service

# Reload systemd
sudo systemctl daemon-reload

# You can now manage containers manually with docker-compose
```

## Advanced Configuration

### Monitoring with Systemd

```bash
# Watch service status in real-time
watch -n 2 'sudo systemctl status fe-lnf'

# View service dependencies
systemctl list-dependencies fe-lnf

# Check when service was last started
systemctl show fe-lnf --property=ActiveEnterTimestamp
```

### Email Notifications on Failure (optional)

Install `systemd-email` or configure with `OnFailure=`:

```bash
# Install mailx
sudo apt-get install mailutils

# Create notification script
cat > /usr/local/bin/fe-lnf-notify.sh << 'EOF'
#!/bin/bash
echo "fe-lnf service has failed!" | mail -s "fe-lnf Alert" your-email@example.com
EOF

chmod +x /usr/local/bin/fe-lnf-notify.sh

# Add to service file under [Service]:
# OnFailure=fe-lnf-notify.service
```

## Comparison: systemd vs manual docker-compose

| Action | With systemd | Manual docker-compose |
|--------|-------------|----------------------|
| Start on boot | Automatic | Manual |
| Restart on failure | Automatic (10s delay) | Manual |
| Start app | `sudo systemctl start fe-lnf` | `docker-compose up -d` |
| Stop app | `sudo systemctl stop fe-lnf` | `docker-compose down` |
| View logs | `sudo journalctl -u fe-lnf -f` | `docker-compose logs -f` |
| Check status | `sudo systemctl status fe-lnf` | `docker-compose ps` |

## Important Notes

- ✅ Systemd manages docker-compose, which manages containers
- ✅ You can still use `docker-compose` commands directly
- ✅ The service reads environment from `.env` file
- ✅ Restart delay is 10 seconds (configurable via `RestartSec`)
- ⚠️ Container logs are still in Docker, not systemd
- ⚠️ Make sure `.env` file exists before starting service
