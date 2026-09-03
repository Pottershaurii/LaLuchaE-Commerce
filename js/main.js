// ========== DATOS DE PRODUCTOS ==========
const productos = [
    { id: 1, nombre: "Butifarra", categoria: "carnivoros", precio: 14.90, descripcion: "Pan francés con butifarra, cebolla y sarza criolla.", imagen: "https://placehold.co/200x140/eee/333?text=Butifarra" },
    { id: 2, nombre: "Chicharrón", categoria: "carnivoros", precio: 14.90, descripcion: "Chicharrón criollo con camote y sarza.", imagen: "https://placehold.co/200x140/eee/333?text=Chicharrón" },
    { id: 3, nombre: "Arado", categoria: "carnivoros", precio: 15.90, descripcion: "Pan con jamón, queso y huevo frito.", imagen: "https://placehold.co/200x140/eee/333?text=Arado" },
    { id: 4, nombre: "Lomo Saltado", categoria: "carnivoros", precio: 18.90, descripcion: "Lomo saltado con cebolla, tomate y papas.", imagen: "https://placehold.co/200x140/eee/333?text=Lomo" },
    { id: 5, nombre: "Pollo Criollo", categoria: "omnivoros", precio: 15.90, descripcion: "Pollo a la plancha con ensalada fresca.", imagen: "https://placehold.co/200x140/eee/333?text=Pollo" },
    { id: 6, nombre: "Lachón Homemão", categoria: "carnivoros", precio: 17.50, descripcion: "Cocho horneado con especias y salsa criolla.", imagen: "https://placehold.co/200x140/eee/333?text=Lachón" },
    { id: 7, nombre: "Veggie Hummus", categoria: "herbivoros", precio: 12.50, descripcion: "Hummus de garbanzo con vegetales asados.", imagen: "https://placehold.co/200x140/eee/333?text=Hummus" },
    { id: 8, nombre: "Milanesa Napolitana", categoria: "omnivoros", precio: 16.90, descripcion: "Milanesa de carne con jamón, queso y salsa de tomate.", imagen: "https://placehold.co/200x140/eee/333?text=Milanesa" },
];

// ========== RENDERIZAR PRODUCTOS ==========
const grid = document.getElementById('grid-productos');

function renderizarProductos(lista) {
    grid.innerHTML = lista.map(p => `
        <article class="producto-card" data-categoria="${p.categoria}">
            <img src="${p.imagen}" alt="${p.nombre}" loading="lazy">
            <h3>${p.nombre}</h3>
            <p class="descripcion-producto">${p.descripcion}</p>
            <span class="categoria">${p.categoria}</span>
            <span class="precio">S/ ${p.precio.toFixed(2)}</span>
            <button class="btn-agregar" data-id="${p.id}">Agregar</button>
        </article>
    `).join('');

    // Eventos para botones "Agregar"
    document.querySelectorAll('.btn-agregar').forEach(btn => {
        btn.addEventListener('click', () => {
            const id = parseInt(btn.dataset.id);
            const producto = productos.find(p => p.id === id);
            if (producto) agregarAlCarrito(producto);
        });
    });
}

// ========== FILTROS ==========
function filtrarProductos(categoria) {
    const lista = categoria === 'todos'
        ? productos
        : productos.filter(p => p.categoria === categoria);
    renderizarProductos(lista);
}

document.querySelectorAll('.filtro-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        document.querySelectorAll('.filtro-btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        filtrarProductos(btn.dataset.filtro);
    });
});

// ========== BUSCADOR ==========
const inputBuscar = document.getElementById('input-buscar');
const btnBuscar = document.getElementById('btn-buscar');

function buscarProductos(texto) {
    const filtrados = productos.filter(p =>
        p.nombre.toLowerCase().includes(texto.toLowerCase()) ||
        p.descripcion.toLowerCase().includes(texto.toLowerCase())
    );
    renderizarProductos(filtrados);
}

btnBuscar.addEventListener('click', () => {
    const texto = inputBuscar.value.trim();
    if (texto) buscarProductos(texto);
});
inputBuscar.addEventListener('keyup', (e) => {
    if (e.key === 'Enter') {
        const texto = inputBuscar.value.trim();
        if (texto) buscarProductos(texto);
    }
});

// ========== CARRITO ==========
let carrito = [];

function agregarAlCarrito(producto) {
    carrito.push(producto);
    actualizarContador();
    alert(`🍔 ${producto.nombre} agregado al carrito`);
}

function actualizarContador() {
    document.getElementById('contador-carrito').textContent = carrito.length;
}

// ========== INICIALIZAR ==========
renderizarProductos(productos);
document.querySelector('.filtro-btn[data-filtro="todos"]')?.classList.add('active');

// Navegación principal (resaltar enlace activo)
document.querySelectorAll('.navegacion-principal a').forEach(link => {
    link.addEventListener('click', function(e) {
        document.querySelectorAll('.navegacion-principal a').forEach(l => l.classList.remove('active'));
        this.classList.add('active');
    });
});