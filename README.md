SlashAlive
==========

Displays currently "alive" players on the server. Offline or online. Displays RPEngine RP Name if found.

![gif-1](https://i.imgur.com/811emvq.gif)

![gif-2](https://i.imgur.com/80eMvNS.gif)

## Commands

* `/alive [page]` - Displays alive players
* `/alive remove <username>` - Forcibly removes a player from the database. They will be re-added if the log in again
* `/alive add <username>` - Forcibly adds a player to the database. They will be removed if they die.

## Permissions

* `slashalive.alive` - Gives access to `/alive` command

## Notes

* Players will not be re-added to the list if they respawn (since this is meant for hardcore anyways)
* If a player does happen to be revived, they will be added back to the list next time they log in with more than 0 health

*moon2CUTE*
