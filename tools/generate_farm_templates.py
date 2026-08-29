"""
Builds the mod's village farm templates.

Two shapes. The copied ones are the vanilla plains farms: two columns of crops, a water channel on every third,
a composter at one corner and a jigsaw where the street attaches. That layout is the one worth
keeping - it is the only vanilla farm laid out in lanes at all - so the other village types get it
too, dressed in their own materials instead of their own irregular fields.

The blob ones are grown here rather than copied: a wobbled circle of field around a pond, with no
straight edge anywhere. They are what keeps a village from looking like a grid of identical plots.

Run from the repository root with the vanilla plains farm templates alongside:

    python3 tools/generate_farm_templates.py <dir holding plains_small_farm_1.nbt and plains_large_farm_1.nbt>

Vanilla templates come from the version's own data, e.g. the 1.20.6-data branch of misode/mcmeta.
The output is committed, so this only has to be run when a layout changes.
"""

import math
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

# The blob farms, which are grown rather than copied. Radius is in blocks from the middle; the two
# wobble terms are what keep the edge from reading as a circle. Fixed numbers rather than a random
# seed, so the same file comes out of every run.
BLOBS = {
    'blob_small_farm': {'size': 9, 'radius': 3.4, 'ponds': [(0, 0)]},
    'blob_large_farm': {'size': 13, 'radius': 5.2, 'ponds': [(-2, -1), (2, 1)]},
}


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


def edge(angle, radius):
    """The blob's outline: a circle pushed in and out so no two sides match."""
    return radius + 0.9 * math.sin(3 * angle) + 0.55 * math.cos(5 * angle + 1.2)


def blob_field(shape):
    """Which cells are field, and which of those hold water, keyed by (x, z)."""
    size = shape['size']
    middle = (size - 1) / 2
    field = {}

    for z in range(size):
        for x in range(size):
            dx = x - middle
            dz = z - middle
            distance = math.hypot(dx, dz)

            if distance > edge(math.atan2(dz, dx), shape['radius']):
                continue

            # A pond rather than a channel. Every plot still wants water within four blocks, which
            # one pond covers on the small field and two on the large.
            pond = any(math.hypot(dx - px, dz - pz) < 1.3 for px, pz in shape['ponds'])
            field[(x, z)] = 'water' if pond else 'crop'

    return field


def blob(shape, biome):
    """A farm with no straight edges, built block by block rather than copied from vanilla."""
    size = shape['size']
    field = blob_field(shape)
    ground = biome['ground'].get('minecraft:grass_block') if biome['ground'] else None
    ground = entry(*ground) if ground else entry('minecraft:grass_block', {'snowy': 'false'})

    states = {}
    blocks = []

    def put(x, y, z, state, extra=None):
        key = repr(state)

        if key not in states:
            states[key] = (len(states), state)

        made = {'pos': [x, y, z], 'state': states[key][0]}

        if extra:
            made['nbt'] = extra

        blocks.append(made)

    for z in range(size):
        for x in range(size):
            kind = field.get((x, z))

            if kind == 'water':
                put(x, 0, z, entry('minecraft:water', {'level': '0'}))
            elif kind == 'crop':
                put(x, 0, z, entry('minecraft:farmland', {'moisture': '7'}))
                put(x, 1, z, entry('minecraft:wheat', {'age': str((x * 3 + z * 5) % 8)}))
            else:
                continue

            # Air above, so the field is not left standing inside whatever grew there before.
            for y in range(2 if kind == 'crop' else 1, 4):
                put(x, y, z, entry('minecraft:air', None))

    # The way in. Placed on the western edge at the middle row, the same edge and facing the copied
    # farms use, with a composter beside it so the plot reads as somebody's work.
    entrance = min((cell for cell in field if cell[1] == (size - 1) // 2), key=lambda cell: cell[0])
    put(entrance[0] - 1, 0, entrance[1], entry('minecraft:jigsaw', {'orientation': 'west_up'}), {
        'final_state': biome['final_state'],
        'name': 'minecraft:building_entrance',
        'pool': biome['pool'],
        'joint': 'aligned',
        'id': 'minecraft:jigsaw',
        'target': 'minecraft:building_entrance',
    })
    put(entrance[0] - 1, 0, entrance[1] - 1, ground)
    put(entrance[0] - 1, 1, entrance[1] - 1, entry('minecraft:composter', {'level': '0'}))

    palette = [state for _, state in sorted(states.values())]

    return {
        'size': [size, 4, size],
        'entities': [],
        'blocks': blocks,
        'palette': palette,
        'DataVersion': nbt.Tag(3, 3839),
    }


def main(vanilla):
    for name, source in SOURCES.items():
        template = nbt.load(os.path.join(vanilla, source))

        for biome, dressing in BIOMES.items():
            folder = os.path.join(OUT, biome)
            os.makedirs(folder, exist_ok=True)
            nbt.save(os.path.join(folder, name + '.nbt'), reskin(template, dressing))
            print('wrote', os.path.join(folder, name + '.nbt'))

    for name, shape in BLOBS.items():
        for biome, dressing in BIOMES.items():
            folder = os.path.join(OUT, biome)
            os.makedirs(folder, exist_ok=True)
            nbt.save(os.path.join(folder, name + '.nbt'), blob(shape, dressing))
            print('wrote', os.path.join(folder, name + '.nbt'))


if __name__ == '__main__':
    main(sys.argv[1])
