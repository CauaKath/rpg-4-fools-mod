"""
Builds the mod's village farm templates.

The layouts are the vanilla plains farms: two columns of crops, a water channel on every third,
a composter at one corner and a jigsaw where the street attaches. That layout is the one worth
keeping - it is the only vanilla farm laid out in lanes at all - so the other village types get it
too, dressed in their own materials instead of their own irregular fields.

Run from the repository root with the vanilla plains farm templates alongside:

    python3 tools/generate_farm_templates.py <dir holding plains_small_farm_1.nbt and plains_large_farm_1.nbt>

Vanilla templates come from the version's own data, e.g. the 1.20.6-data branch of misode/mcmeta.
The output is committed, so this only has to be run when a layout changes.
"""

import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import nbt

OUT = 'src/main/resources/data/rpg4fools/structures/village'

# Per village type: what the border is made of, what the ground under the field is, and what the
# jigsaw leaves behind. Plains keeps vanilla's street pool so a farm can still grow a street off
# itself; the others use the empty pool their own vanilla farms use.
BIOMES = {
    'plains': {
        'border': ('minecraft:oak_log', {'axis': 'y'}),
        'ground': None,
        'final_state': 'minecraft:oak_log',
        'pool': 'minecraft:village/plains/streets',
    },
    'savanna': {
        'border': ('minecraft:acacia_log', {'axis': 'y'}),
        'ground': None,
        'final_state': 'minecraft:acacia_log',
        'pool': 'minecraft:empty',
    },
    'taiga': {
        'border': ('minecraft:spruce_log', {'axis': 'y'}),
        'ground': None,
        'final_state': 'minecraft:spruce_log',
        'pool': 'minecraft:empty',
    },
    'snowy': {
        'border': ('minecraft:spruce_log', {'axis': 'y'}),
        'ground': {'minecraft:grass_block': ('minecraft:grass_block', {'snowy': 'true'})},
        'final_state': 'minecraft:spruce_log',
        'pool': 'minecraft:empty',
    },
    'desert': {
        'border': ('minecraft:cut_sandstone', None),
        'ground': {
            'minecraft:dirt': ('minecraft:sand', None),
            'minecraft:grass_block': ('minecraft:sand', None),
        },
        'final_state': 'minecraft:cut_sandstone',
        'pool': 'minecraft:empty',
    },
}

SOURCES = {'small_farm': 'plains_small_farm_1.nbt', 'large_farm': 'plains_large_farm_1.nbt'}


def entry(name, properties):
    made = {'Name': name}

    if properties:
        made['Properties'] = dict(properties)

    return made


def reskin(template, biome):
    palette = []

    for state in template['palette']:
        name = state['Name']

        if name == 'minecraft:oak_log':
            palette.append(entry(*biome['border']))
            continue

        if biome['ground'] and name in biome['ground']:
            palette.append(entry(*biome['ground'][name]))
            continue

        palette.append(dict(state))

    blocks = []

    for block in template['blocks']:
        made = {'pos': list(block['pos']), 'state': block['state']}

        if 'nbt' in block:
            jigsaw = dict(block['nbt'])
            jigsaw['final_state'] = biome['final_state']
            jigsaw['pool'] = biome['pool']
            made['nbt'] = jigsaw

        blocks.append(made)

    return {
        'size': list(template['size']),
        'entities': [],
        'blocks': blocks,
        'palette': palette,
        'DataVersion': nbt.Tag(3, template['DataVersion']),
    }


def main(vanilla):
    for name, source in SOURCES.items():
        template = nbt.load(os.path.join(vanilla, source))

        for biome, dressing in BIOMES.items():
            folder = os.path.join(OUT, biome)
            os.makedirs(folder, exist_ok=True)
            nbt.save(os.path.join(folder, name + '.nbt'), reskin(template, dressing))
            print('wrote', os.path.join(folder, name + '.nbt'))


if __name__ == '__main__':
    main(sys.argv[1])
