# MiniFactions Crafting Guide

This guide explains how to craft the various special items in the MiniFactions plugin.

## Crafting Key
The following letters are used in crafting recipes:

### Core Blocks
- **D** = Diamond
- **O** = Obsidian
- **B** = Beacon
- **I** = Iron Block
- **G** = Gold Block
- **E** = Emerald Block
- **N** = Netherite Scrap
- **X** = Netherite Ingot
- **Z** = Netherite Block

### Claim Blocks
- **E** = Emerald
- **R** = Redstone Block
- **D** = Diamond

### Defense Blocks
- **O** = Obsidian
- **I** = Iron Ingot/Block
- **C** = Crying Obsidian
- **G** = Gold Block
- **D** = Diamond Block

### Explosives
- **G** = Gunpowder
- **B** = Stone Button
- **R** = Redstone/Redstone Block
- **P** = Blaze Powder

## Core Block
The Core Block is the central element of your clan's territory. It defines your buildable area and determines how many Defense and Claim blocks you can place.

### How to Get:
Core Blocks are automatically given to clan leaders when a clan is created. If your Core Block is destroyed by an enemy, you will automatically receive a new one.

### Properties (Level 1):
- Area: 10 blocks radius
- Defense Slots: 5
- Claim Slots: 2
- Door Slots: 1
- Member Slots: 10

## Claim Block (Level 1)
Claim Blocks generate points over time for your clan.

### Recipe:
```
EEE
ERE
EEE
```
Where:
- E = Emerald
- R = Redstone Block

### Properties:
- Points per day: 100
- Must be placed within your clan's area of influence

## Defense Block (Tier 1)
Defense Blocks can only be broken by explosives of equal or higher tier.

### Recipe:
```
OOO
OIO
OOO
```
Where:
- O = Obsidian
- I = Iron Ingot

### Properties:
- Cost: 100 points
- Decay time: 24 hours
- Can only be damaged by Tier 1 or higher explosives

## Explosive (Tier 1)
Explosives are used to damage Defense Blocks and Core Blocks during raids.

### Recipe:
```
GBG
BRB
GBG
```
Where:
- G = Gunpowder
- B = Stone Button
- R = Redstone Block

### Properties:
- Fuse time: 5 seconds
- Can damage Tier 1 or lower Defense Blocks
- Place on a Defense Block to activate

## Clan Door
Clan Doors can only be opened by members of your clan.

### Recipe:
```
II
IRI
II
```
Where:
- I = Iron Ingot
- R = Redstone

### Properties:
- Can only be opened by clan members
- Must be placed within your clan's area of influence

## Upgrading Items

### Core Block
To upgrade your Core Block, you need to use the `/clan core upgrade` command while standing near your Core Block. This requires points based on the upgrade cost. Upgrades are done through the GUI and points are combined for the upgrade.

### Claim Block
Claim Blocks can be upgraded by crafting higher-level versions and replacing the existing ones.

## Higher Tier/Level Items

As you progress, you'll be able to craft higher tier/level items with more valuable materials. The crafting patterns remain the same, but the materials change.

Core Blocks are upgraded through the GUI using clan points, not through crafting.

Check the in-game configuration for specific recipes for each level/tier of other items.
