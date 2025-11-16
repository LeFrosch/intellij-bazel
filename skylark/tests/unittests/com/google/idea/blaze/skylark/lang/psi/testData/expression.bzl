expression = (
    f(
        +g(-h(1, 2), **{"x": 3}),
        kw1 = {"k": [i for i in data if i % 2 == 0]},
        kw2 = [x * y for x in xs for y in ys if x not in banned and y in allowed],
        bin = r"bytes",
        *(u if a and not b else v)
    ).attr["k1"][1:4:2].call(lambda p, q = 1, *, r, **kw: (p + q if r else kw["d"])),
)
