const revealItems = document.querySelectorAll(".reveal");
const filterButtons = document.querySelectorAll("[data-filter]");
const searchInput = document.querySelector("[data-search-input]");
const cartCount = document.querySelector("[data-cart-count]");
const cartItems = document.querySelector("[data-cart-items]");
const cartTotal = document.querySelector("[data-cart-total]");
const cartPanel = document.getElementById("cart-panel");
const cartToggles = document.querySelectorAll("[data-cart-toggle]");
const emptyState = document.querySelector("[data-empty-state]");
const productsGrid = document.querySelector("[data-products-grid]");
const heroFeatured = document.querySelector(".hero-featured");

const cart = [];
let activeFilter = "all";
const priceFormatter = new Intl.NumberFormat("en-IN");

function attachRevealAnimation(items) {
  if ("IntersectionObserver" in window) {
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add("is-visible");
            observer.unobserve(entry.target);
          }
        });
      },
      { threshold: 0.18 }
    );

    items.forEach((item, index) => {
      item.style.transitionDelay = `${index * 60}ms`;
      observer.observe(item);
    });
  } else {
    items.forEach((item) => item.classList.add("is-visible"));
  }
}

attachRevealAnimation(revealItems);

function renderProducts() {
  const products = StoreData.getProducts();

  productsGrid.innerHTML = products
    .map(
      (product) => `
        <article class="product-card reveal" data-category="${product.category}" data-name="${product.name}">
          <div class="product-art ${product.gradient}">
            ${
              product.image
                ? `<img src="${product.image}" alt="${product.name}" class="product-image" />`
                : ""
            }
            <span>${product.label}</span>
          </div>
          <div class="product-info">
            <div>
              <p class="product-meta">${product.category} picks</p>
              <h3>${product.name}</h3>
              <p>${product.description}</p>
            </div>
            <div class="product-footer">
              <strong>Rs. ${priceFormatter.format(product.price)}</strong>
              <button class="button button-secondary" type="button" data-add-to-cart="${product.name}" data-price="${product.price}">
                Add to cart
              </button>
            </div>
          </div>
        </article>
      `
    )
    .join("");

  attachRevealAnimation(productsGrid.querySelectorAll(".reveal"));
}

function renderCart() {
  const totalItems = cart.reduce((sum, item) => sum + item.quantity, 0);
  const totalPrice = cart.reduce((sum, item) => sum + item.price * item.quantity, 0);

  cartCount.textContent = totalItems;
  cartTotal.textContent = `Rs. ${priceFormatter.format(totalPrice)}`;

  if (!cart.length) {
    cartItems.innerHTML = '<p class="cart-empty">Your cart is empty. Add a few favorites to get started.</p>';
    return;
  }

  cartItems.innerHTML = cart
    .map(
      (item) => `
        <div class="cart-line">
          <div>
            <strong>${item.name}</strong>
            <p>Qty ${item.quantity}</p>
          </div>
          <span>Rs. ${priceFormatter.format(item.price * item.quantity)}</span>
        </div>
      `
    )
    .join("");
}

function updateProducts() {
  const query = searchInput.value.trim().toLowerCase();
  const productCards = productsGrid.querySelectorAll(".product-card");
  let visibleCount = 0;

  productCards.forEach((card) => {
    const category = card.dataset.category;
    const name = card.dataset.name.toLowerCase();
    const matchesFilter = activeFilter === "all" || category === activeFilter;
    const matchesQuery = !query || name.includes(query);
    const shouldShow = matchesFilter && matchesQuery;

    card.hidden = !shouldShow;

    if (shouldShow) {
      visibleCount += 1;
    }
  });

  emptyState.hidden = visibleCount !== 0;
}

function toggleCart(forceOpen) {
  const shouldOpen = typeof forceOpen === "boolean" ? forceOpen : cartPanel.hidden;
  cartPanel.hidden = !shouldOpen;
  document.body.classList.toggle("cart-open", shouldOpen);

  cartToggles.forEach((toggle) => {
    toggle.setAttribute("aria-expanded", String(shouldOpen));
  });
}

function addToCart(name, price) {
  const existingItem = cart.find((item) => item.name === name);

  if (existingItem) {
    existingItem.quantity += 1;
  } else {
    cart.push({ name, price, quantity: 1 });
  }

  renderCart();
  toggleCart(true);
}

filterButtons.forEach((button) => {
  button.addEventListener("click", () => {
    activeFilter = button.dataset.filter;

    filterButtons.forEach((pill) => pill.classList.remove("is-active"));
    button.classList.add("is-active");
    updateProducts();
  });
});

searchInput.addEventListener("input", updateProducts);

productsGrid.addEventListener("click", (event) => {
  const button = event.target.closest("[data-add-to-cart]");

  if (!button) {
    return;
  }

  addToCart(button.dataset.addToCart, Number(button.dataset.price));
});

if (heroFeatured) {
  heroFeatured.addEventListener("click", (event) => {
    const button = event.target.closest("[data-add-to-cart]");

    if (!button) {
      return;
    }

    addToCart(button.dataset.addToCart, Number(button.dataset.price));
  });
}

cartToggles.forEach((button) => {
  button.addEventListener("click", () => toggleCart());
});

document.addEventListener("keydown", (event) => {
  if (event.key === "Escape" && !cartPanel.hidden) {
    toggleCart(false);
  }
});

document.querySelector(".newsletter-form")?.addEventListener("submit", (event) => {
  event.preventDefault();
  const emailField = event.currentTarget.querySelector("input");
  emailField.value = "";
  emailField.placeholder = "Thanks for joining";
});

window.addEventListener("storage", (event) => {
  if (event.key === "northstar-products") {
    renderProducts();
    updateProducts();
  }
});

window.addEventListener("northstar-products-updated", () => {
  renderProducts();
  updateProducts();
});

renderProducts();
renderCart();
updateProducts();
