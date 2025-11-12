CC_INFO = "@@rules_cc+//cc/private:cc_info.bzl%CcInfo"

def toString(obj):
    if type(obj) == "depset":
        return obj.to_list()
    else:
        return str(obj)

def format(target):
    # return json.encode(providers(target)[CC_INFO])
    # return json.encode(providers(target)[CC_INFO], toString = str)
    return json.encode(providers(target)[CC_INFO], toString = toString)

