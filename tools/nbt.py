import gzip, struct, io

def read(f):
    def u1(): return f.read(1)[0]
    def u2(): return struct.unpack('>h', f.read(2))[0]
    def u4(): return struct.unpack('>i', f.read(4))[0]
    def u8(): return struct.unpack('>q', f.read(8))[0]
    def name():
        n = struct.unpack('>H', f.read(2))[0]
        return f.read(n).decode('utf-8')
    def payload(t):
        if t == 1: return struct.unpack('>b', f.read(1))[0]
        if t == 2: return u2()
        if t == 3: return u4()
        if t == 4: return u8()
        if t == 5: return struct.unpack('>f', f.read(4))[0]
        if t == 6: return struct.unpack('>d', f.read(8))[0]
        if t == 7: return f.read(u4())
        if t == 8: return name()
        if t == 9:
            it = u1(); n = u4()
            return [payload(it) for _ in range(n)]
        if t == 10:
            d = {}
            while True:
                nt = u1()
                if nt == 0: return d
                k = name()
                d[k] = payload(nt)
        if t == 11: return [u4() for _ in range(u4())]
        if t == 12: return [u8() for _ in range(u4())]
        raise ValueError(t)
    assert u1() == 10
    name()
    return payload(10)

def load(path):
    with gzip.open(path, 'rb') as g:
        return read(io.BytesIO(g.read()))

# --- writing ---

def _name(s):
    b = s.encode('utf-8')
    return struct.pack('>H', len(b)) + b

def _payload(v):
    """Types are inferred: dict->compound, list->list, str->string, int->int, Tag wrappers force a type."""
    if isinstance(v, Tag):
        return v.tag, v.encode()
    if isinstance(v, dict):
        out = b''
        for k, sub in v.items():
            t, p = _payload(sub)
            out += bytes([t]) + _name(k) + p
        return 10, out + b'\x00'
    if isinstance(v, list):
        if not v:
            return 9, bytes([0]) + struct.pack('>i', 0)
        types, payloads = zip(*(_payload(e) for e in v))
        assert len(set(types)) == 1, types
        return 9, bytes([types[0]]) + struct.pack('>i', len(v)) + b''.join(payloads)
    if isinstance(v, str):
        return 8, _name(v)
    if isinstance(v, int):
        return 3, struct.pack('>i', v)
    raise ValueError(type(v))

class Tag:
    """Forces a specific NBT tag type where the Python type is ambiguous."""
    def __init__(self, tag, value):
        self.tag = tag
        self.value = value

    def encode(self):
        if self.tag == 3:
            return struct.pack('>i', self.value)
        raise ValueError(self.tag)

def save(path, root):
    tag, payload = _payload(root)
    assert tag == 10
    with gzip.open(path, 'wb') as g:
        g.write(bytes([10]) + _name('') + payload)
