# Alternative Keepalive Fabric
Alternative Keepalive simple fabric mod that ports Purpur's Alternative Keepalive patch to Mixin.


### To quote the purpur documentation:

"[use-alternate-keepalive is a] a different approach to keepalive ping timeouts. ...[T]his sends a keepalive packet 
once per second to a player, and only kicks for timeout if none of them were responded to in 30 seconds. Responding to 
any of them in any order will keep the player connected. AKA, it won't kick your players because 1 packet gets dropped 
somewhere along the lines."

(https://purpurmc.org/docs/Configuration/#use-alternate-keepalive)
