const revealVendorItems = document.querySelectorAll(".reveal");
const loginForm = document.querySelector("[data-login-form]");
const loginMessage = document.querySelector("[data-login-message]");
const dashboard = document.querySelector("[data-dashboard]");
const vendorLayout = document.querySelector(".vendor-layout");
const productForm = document.querySelector("[data-product-form]");
const productMessage = document.querySelector("[data-product-message]");
const productList = document.querySelector("[data-vendor-products]");
const productCount = document.querySelector("[data-product-count]");
const logoutButton = document.querySelector("[data-logout]");
const resetButton = document.querySelector("[data-reset-products]");
const imageInput = document.querySelector("[data-image-input]");
const imagePreview = document.querySelector("[data-image-preview]");
const priceFormatterVendor = new Intl.NumberFormat("en-IN");

const VENDOR_EMAIL = "vendor@northstar.in";
const VENDOR_PASSWORD = "admin123";
let uploadedImageData = "";

if ("IntersectionObserver" in window) {
  const revealObserver = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add("is-visible");
          revealObserver.unobserve(entry.target);
        }
      });
    },
    { threshold: 0.18 }
  );

  revealVendorItems.forEach((item, index) => {
    item.style.transitionDelay = `${index * 70}ms`;
    revealObserver.observe(item);
  });
} else {
  revealVendorItems.forEach((item) => item.classList.add("is-visible"));
}

function setDashboardState(isLoggedIn) {
  dashboard.hidden = !isLoggedIn;
  loginForm.closest(".vendor-auth-card").hidden = isLoggedIn;
  vendorLayout.classList.toggle("is-dashboard-only", isLoggedIn);
}

function setImagePreview(src) {
  uploadedImageData = src || "";

  if (!uploadedImageData) {
    imagePreview.innerHTML = "<span>No image selected yet</span>";
    return;
  }

  imagePreview.innerHTML = `<img src="${uploadedImageData}" alt="Selected product preview" />`;
}

function renderVendorProducts() {
  const products = StoreData.getProducts();
  productCount.textContent = `${products.length} products`;

  productList.innerHTML = products
    .map(
      (product) => `
        <article class="vendor-product-item">
          <div class="vendor-product-art ${product.gradient}">
            ${product.image ? `<img src="${product.image}" alt="${product.name}" />` : `<span>${product.label}</span>`}
          </div>
          <div class="vendor-product-copy">
            <p class="product-meta">${product.category}</p>
            <h4>${product.name}</h4>
            <p>${product.description}</p>
            <div class="vendor-product-foot">
              <strong>Rs. ${priceFormatterVendor.format(product.price)}</strong>
              <button class="button button-secondary" type="button" data-remove-product="${product.id}">Remove</button>
            </div>
          </div>
        </article>
      `
    )
    .join("");
}

function showMessage(target, message, isError = false) {
  target.textContent = message;
  target.dataset.state = message ? (isError ? "error" : "success") : "";
}

function readFileAsDataUrl(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result || ""));
    reader.onerror = () => reject(new Error("Image upload failed."));
    reader.readAsDataURL(file);
  });
}

loginForm.addEventListener("submit", (event) => {
  event.preventDefault();
  const formData = new FormData(loginForm);
  const email = String(formData.get("email") || "").trim().toLowerCase();
  const password = String(formData.get("password") || "").trim();

  if (email !== VENDOR_EMAIL || password !== VENDOR_PASSWORD) {
    showMessage(loginMessage, "Login failed. Use the demo vendor credentials shown above.", true);
    return;
  }

  StoreData.setVendorSession(true);
  loginForm.reset();
  showMessage(loginMessage, "");
  setDashboardState(true);
  renderVendorProducts();
});

imageInput.addEventListener("change", async () => {
  const [file] = imageInput.files || [];

  if (!file) {
    setImagePreview("");
    return;
  }

  try {
    const dataUrl = await readFileAsDataUrl(file);
    setImagePreview(dataUrl);
    showMessage(productMessage, "");
  } catch {
    setImagePreview("");
    showMessage(productMessage, "Could not read that image file. Try another picture.", true);
  }
});

productForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  const formData = new FormData(productForm);

  if (imageInput.files && imageInput.files[0] && !uploadedImageData) {
    try {
      uploadedImageData = await readFileAsDataUrl(imageInput.files[0]);
    } catch {
      showMessage(productMessage, "Could not save the product image. Try again.", true);
      return;
    }
  }

  StoreData.addProduct({
    name: String(formData.get("name") || "").trim(),
    category: String(formData.get("category") || "").trim(),
    label: String(formData.get("label") || "").trim(),
    description: String(formData.get("description") || "").trim(),
    price: Number(formData.get("price")),
    gradient: String(formData.get("gradient") || "").trim(),
    image: uploadedImageData,
  });

  productForm.reset();
  setImagePreview("");
  showMessage(productMessage, "Product added. It is now visible on the customer storefront.");
  renderVendorProducts();
});

productList.addEventListener("click", (event) => {
  const button = event.target.closest("[data-remove-product]");

  if (!button) {
    return;
  }

  StoreData.removeProduct(button.dataset.removeProduct);
  renderVendorProducts();
  showMessage(productMessage, "Product removed from the storefront.");
});

logoutButton.addEventListener("click", () => {
  StoreData.setVendorSession(false);
  showMessage(productMessage, "");
  setDashboardState(false);
});

resetButton.addEventListener("click", () => {
  StoreData.resetProducts();
  renderVendorProducts();
  showMessage(productMessage, "Catalog reset to the default products.");
});

setDashboardState(StoreData.isVendorLoggedIn());
setImagePreview("");

if (StoreData.isVendorLoggedIn()) {
  renderVendorProducts();
}

window.addEventListener("northstar-products-updated", () => {
  if (StoreData.isVendorLoggedIn()) {
    renderVendorProducts();
  }
});
