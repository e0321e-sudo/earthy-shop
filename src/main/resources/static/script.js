const productImages = [
  ["sunset-sea", "sunset sea", "./assets/products/sunset-sea.jpeg"],
  ["field-town", "field town", "./assets/products/field-town.jpeg"],
  ["cloud-field", "cloud field", "./assets/products/cloud-field.jpeg"],
  ["grazing-horse", "grazing horse", "./assets/products/grazing-horse.jpeg"],
  ["green-valley", "green valley", "./assets/products/green-valley.jpeg"],
  ["tidal-beach", "tidal beach", "./assets/products/tidal-beach.jpeg"],
  ["road-field", "road field", "./assets/products/road-field.jpeg"],
  ["forest-walk", "forest walk", "./assets/products/forest-walk.jpeg"],
];

const products = [
  ...productImages.map(([id, name, image]) => ({
    id: `${id}-postcard`,
    category: "postcard",
    type: "POSTCARD",
    name: `${name} postcard`,
    price: 3500,
    image,
    imageClass: "photo",
  })),
  ...productImages.map(([id, name, image]) => ({
    id: `${id}-poster`,
    category: "poster",
    type: "POSTER",
    name: `${name} poster`,
    price: 18000,
    image,
    imageClass: "photo",
    hasFrame: true,
  })),
];

const cartItems = [];
let currentView = "home";
let currentProduct = products[0];
let quantity = 1;

const cartCount = document.querySelector("#cartCount");
const cartList = document.querySelector("#cartItems");
const cartTotal = document.querySelector("#cartTotal");
const cartPanel = document.querySelector(".cart-panel");
const navLinks = document.querySelectorAll("[data-view]");
const heroSection = document.querySelector("#home");
const shopSection = document.querySelector("#shop");
const detailSection = document.querySelector("#productDetail");
const aboutSection = document.querySelector("#about");
const shopTitle = document.querySelector("#shopTitle");
const productGrid = document.querySelector("#productGrid");
const detailMedia = document.querySelector("#detailMedia");
const detailType = document.querySelector("#detailType");
const detailName = document.querySelector("#detailName");
const detailPrice = document.querySelector("#detailPrice");
const detailFrameLabel = document.querySelector("#detailFrameLabel");
const detailFrame = document.querySelector("#detailFrame");
const quantityValue = document.querySelector("#quantityValue");
const orderQty = document.querySelector("#orderQty");
const orderTotal = document.querySelector("#orderTotal");

const formatWon = (value) => `₩${value.toLocaleString("ko-KR")}`;

function productMarkup(product) {
  return `
    <div class="product-image">
      <img src="${product.image}" alt="${product.name}" />
    </div>
  `;
}

function renderProducts(view) {
  const visibleProducts = products.filter((product) => {
    if (view === "all") return true;
    if (view === "frame") return product.hasFrame;
    return product.category === view;
  });

  productGrid.innerHTML = visibleProducts
    .map(
      (product) => `
        <article class="product-card" data-product-id="${product.id}">
          ${productMarkup(product)}
          <div class="product-info">
            <span>${product.type}</span>
            <h3>${product.name}</h3>
            <p>${formatWon(product.price)}</p>
          </div>
        </article>
      `
    )
    .join("");
}

function renderCart() {
  const itemCount = cartItems.reduce((sum, item) => sum + item.quantity, 0);
  cartCount.textContent = itemCount > 0 ? itemCount : "";
  cartCount.hidden = itemCount === 0;
  cartPanel.classList.toggle("has-items", cartItems.length > 0);
  cartList.innerHTML = "";

  cartItems.forEach((item) => {
    const li = document.createElement("li");
    li.textContent = `${item.name} x ${item.quantity} · ${formatWon(item.price * item.quantity)}`;
    cartList.appendChild(li);
  });

  const total = cartItems.reduce((sum, item) => sum + item.price * item.quantity, 0);
  cartTotal.textContent = formatWon(total);
}

function setActiveLink(view) {
  document.querySelectorAll(".category-nav a").forEach((link) => {
    link.classList.toggle("is-active", link.dataset.view === view);
  });
}

function showOnly(section) {
  heroSection.hidden = section !== "home";
  shopSection.hidden = section !== "shop";
  detailSection.hidden = section !== "detail";
  aboutSection.hidden = section !== "about";
}

function showView(view) {
  const isShopView = ["all", "postcard", "poster", "frame"].includes(view);
  const titles = {
    all: "ALL",
    postcard: "엽서",
    poster: "포스터",
    frame: "액자",
  };

  currentView = view;

  if (isShopView) {
    shopTitle.textContent = titles[view];
    renderProducts(view);
    showOnly("shop");
  } else if (view === "about") {
    showOnly("about");
  } else {
    showOnly("home");
  }

  setActiveLink(view);
  window.scrollTo({ top: 0, behavior: "smooth" });
}

function selectedUnitPrice() {
  return currentProduct.price + (currentProduct.hasFrame && detailFrame.checked ? 12000 : 0);
}

function updateDetailTotals() {
  quantityValue.textContent = quantity;
  orderQty.textContent = `${quantity}개`;
  orderTotal.textContent = formatWon(selectedUnitPrice() * quantity);
}

function detailImageMarkup(product) {
  return `<img src="${product.image}" alt="${product.name}" />`;
}

function openProduct(productId) {
  currentProduct = products.find((product) => product.id === productId);
  quantity = 1;
  detailFrame.checked = false;

  detailMedia.innerHTML = detailImageMarkup(currentProduct);
  detailType.textContent = currentProduct.type;
  detailName.textContent = currentProduct.name;
  detailPrice.textContent = formatWon(currentProduct.price);
  detailFrameLabel.hidden = !currentProduct.hasFrame;

  updateDetailTotals();
  showOnly("detail");
  window.scrollTo({ top: 0, behavior: "smooth" });
}

function addCurrentProductToCart() {
  const hasFrame = currentProduct.hasFrame && detailFrame.checked;
  cartItems.push({
    name: hasFrame ? `${currentProduct.name} + frame` : currentProduct.name,
    price: selectedUnitPrice(),
    quantity,
  });
  renderCart();
}

navLinks.forEach((link) => {
  link.addEventListener("click", (event) => {
    event.preventDefault();
    showView(link.dataset.view);
  });
});

productGrid.addEventListener("click", (event) => {
  const card = event.target.closest("[data-product-id]");
  if (!card) return;
  openProduct(card.dataset.productId);
});

document.querySelector("#backToList").addEventListener("click", () => showView(currentView));

document.querySelector("#decreaseQty").addEventListener("click", () => {
  quantity = Math.max(1, quantity - 1);
  updateDetailTotals();
});

document.querySelector("#increaseQty").addEventListener("click", () => {
  quantity += 1;
  updateDetailTotals();
});

detailFrame.addEventListener("change", updateDetailTotals);

document.querySelector("#addDetailToCart").addEventListener("click", addCurrentProductToCart);

document.querySelector("#buyNow").addEventListener("click", () => {
  addCurrentProductToCart();
  cartPanel.classList.add("has-items");
});

renderProducts("all");
renderCart();
