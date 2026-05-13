const StoreData = (() => {
  const PRODUCT_STORAGE_KEY = "northstar-products";
  const SESSION_KEY = "northstar-vendor-session";

  const memoryStore = new Map();

  const defaultProducts = [
    {
      id: "cloud-loop-vase",
      name: "Cloud Loop Vase",
      category: "home",
      label: "Home Accent",
      description: "Matte ceramic statement piece designed for single stems, entry consoles, and open shelves.",
      price: 2499,
      gradient: "gradient-peach",
    },
    {
      id: "axis-wireless-pad",
      name: "Axis Wireless Pad",
      category: "tech",
      label: "Charging",
      description: "Fast wireless charger with a brushed aluminum base and soft-touch landing ring for compact desks.",
      price: 3999,
      gradient: "gradient-blue",
    },
    {
      id: "wayfinder-sling",
      name: "Wayfinder Sling",
      category: "travel",
      label: "Carry",
      description: "Weather-resistant sling with modular pockets for everyday commute essentials and short city travel.",
      price: 4799,
      gradient: "gradient-olive",
    },
    {
      id: "halo-desk-lamp",
      name: "Halo Desk Lamp",
      category: "tech",
      label: "Lighting",
      description: "Low-glare task light with dimmable warmth settings for late-night study sessions and focused work.",
      price: 6499,
      gradient: "gradient-amber",
    },
    {
      id: "moss-knit-throw",
      name: "Moss Knit Throw",
      category: "home",
      label: "Textile",
      description: "Chunky recycled-cotton throw that adds softness without visual clutter to sofas and reading corners.",
      price: 3299,
      gradient: "gradient-moss",
    },
    {
      id: "field-bottle-750",
      name: "Field Bottle 750",
      category: "travel",
      label: "Hydration",
      description: "Double-wall stainless steel bottle with powder-coated grip and leakproof lid for daily travel.",
      price: 1899,
      gradient: "gradient-stone",
    },
  ];

  function safeGet(storageType, key) {
    try {
      return window[storageType].getItem(key);
    } catch {
      return memoryStore.has(`${storageType}:${key}`) ? memoryStore.get(`${storageType}:${key}`) : null;
    }
  }

  function safeSet(storageType, key, value) {
    try {
      window[storageType].setItem(key, value);
    } catch {
      memoryStore.set(`${storageType}:${key}`, value);
    }
  }

  function safeRemove(storageType, key) {
    try {
      window[storageType].removeItem(key);
    } catch {
      memoryStore.delete(`${storageType}:${key}`);
    }
  }

  function broadcastProducts(products) {
    window.dispatchEvent(
      new CustomEvent("northstar-products-updated", {
        detail: { products },
      })
    );
  }

  function readProducts() {
    const saved = safeGet("localStorage", PRODUCT_STORAGE_KEY);

    if (!saved) {
      safeSet("localStorage", PRODUCT_STORAGE_KEY, JSON.stringify(defaultProducts));
      return [...defaultProducts];
    }

    try {
      const parsed = JSON.parse(saved);

      if (Array.isArray(parsed) && parsed.length) {
        return parsed;
      }
    } catch {
      // Fall back to defaults below.
    }

    safeSet("localStorage", PRODUCT_STORAGE_KEY, JSON.stringify(defaultProducts));
    return [...defaultProducts];
  }

  function saveProducts(products) {
    safeSet("localStorage", PRODUCT_STORAGE_KEY, JSON.stringify(products));
    broadcastProducts(products);
  }

  function getProducts() {
    return readProducts();
  }

  function addProduct(product) {
    const products = readProducts();
    const nextProduct = {
      ...product,
      id:
        product.id ||
        `${product.name.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/(^-|-$)/g, "")}-${Date.now()}`,
    };

    products.unshift(nextProduct);
    saveProducts(products);
    return nextProduct;
  }

  function removeProduct(id) {
    const products = readProducts().filter((product) => product.id !== id);
    saveProducts(products);
    return products;
  }

  function resetProducts() {
    const products = [...defaultProducts];
    saveProducts(products);
    return products;
  }

  function isVendorLoggedIn() {
    return safeGet("sessionStorage", SESSION_KEY) === "active";
  }

  function setVendorSession(isLoggedIn) {
    if (isLoggedIn) {
      safeSet("sessionStorage", SESSION_KEY, "active");
    } else {
      safeRemove("sessionStorage", SESSION_KEY);
    }
  }

  return {
    defaultProducts,
    getProducts,
    addProduct,
    removeProduct,
    resetProducts,
    isVendorLoggedIn,
    setVendorSession,
  };
})();
