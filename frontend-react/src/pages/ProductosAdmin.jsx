import { useEffect, useState } from 'react'

const API = import.meta.env.VITE_API_URL || 'http://localhost:8080'

const inputStyle = {
  padding: '0.6rem 0.75rem', border: '1px solid #ddd', borderRadius: '6px', fontSize: '0.95rem'
}
const btnPrimary = {
  background: '#c45c26', color: '#fff', border: 'none', padding: '0.6rem 1.2rem',
  borderRadius: '6px', cursor: 'pointer', fontWeight: 500
}
const btnSecondary = {
  background: '#eee', color: '#333', border: 'none', padding: '0.6rem 1.2rem',
  borderRadius: '6px', cursor: 'pointer'
}
const btnSmall = {
  background: 'transparent', border: '1px solid #ddd', padding: '0.3rem 0.6rem',
  borderRadius: '4px', cursor: 'pointer', fontSize: '0.85rem'
}
const thStyle = { padding: '0.75rem', fontWeight: 600, fontSize: '0.85rem' }
const tdStyle = { padding: '0.75rem', fontSize: '0.9rem' }

function ProductosAdmin() {
  const [productos, setProductos] = useState([])
  const [categorias, setCategorias] = useState([])
  const [cargando, setCargando] = useState(true)
  const [form, setForm] = useState({ nombre: '', descripcion: '', precio: '', imagenUrl: '', categoriaId: '', stockMinimo: 5 })
  const [editando, setEditando] = useState(null)
  const [mensaje, setMensaje] = useState(null)

  const token = localStorage.getItem('token')

  const cargar = () => {
    Promise.all([
      fetch(`${API}/api/productos`).then(r => r.json()),
      fetch(`${API}/api/categorias`).then(r => r.json())
    ]).then(([prods, cats]) => {
      setProductos(prods)
      setCategorias(cats)
      setCargando(false)
    }).catch(() => {
      setMensaje({ texto: 'No se pudo cargar la información. Intenta de nuevo.', error: true })
      setCargando(false)
    })
  }

  useEffect(() => { cargar() }, [])

  const guardar = async (e) => {
    e.preventDefault()
    const url = editando
      ? `${API}/api/productos/${editando}`
      : `${API}/api/productos`
    const method = editando ? 'PUT' : 'POST'
    const res = await fetch(url, {
      method,
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
      body: JSON.stringify({ ...form, precio: parseFloat(form.precio), categoriaId: parseInt(form.categoriaId) })
    })
    if (res.ok) {
      setMensaje({ texto: editando ? 'Producto actualizado' : 'Producto creado', error: false })
      setForm({ nombre: '', descripcion: '', precio: '', imagenUrl: '', categoriaId: '', stockMinimo: 5 })
      setEditando(null)
      cargar()
    } else {
      const datos = await res.json().catch(() => ({}))
      setMensaje({ texto: datos.error || datos.message || 'Error al guardar', error: true })
    }
  }

  const desactivar = async (id) => {
    if (!confirm('¿Desactivar este producto?')) return
    const res = await fetch(`${API}/api/productos/${id}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${token}` }
    })
    if (res.ok) {
      setMensaje({ texto: 'Producto desactivado', error: false })
      cargar()
    } else {
      setMensaje({ texto: 'No se pudo desactivar el producto', error: true })
    }
  }

  const editar = (p) => {
    setEditando(p.id)
    setForm({
      nombre: p.nombre,
      descripcion: p.descripcion || '',
      precio: p.precio,
      imagenUrl: p.imagenUrl || '',
      categoriaId: p.categoria.id,
      stockMinimo: p.stockMinimo
    })
  }

  if (cargando) return <p>Cargando…</p>

  return (
    <div>
        <h1 style={{ marginBottom: '1.5rem', fontWeight: 600 }}>Productos</h1>
        {mensaje && (
        <p style={{
            background: mensaje.error ? '#fdecea' : '#e8f5e9',
            color: mensaje.error ? '#c62828' : '#2e7d32',
            padding: '0.75rem 1rem',
            borderRadius: '6px', marginBottom: '1rem'
        }}>{mensaje.texto}</p>
        )}
        <form onSubmit={guardar} style={{
        display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem', maxWidth: '100%',
        background: '#fff', padding: '1.5rem', borderRadius: '8px',
        boxShadow: '0 1px 3px rgba(0,0,0,0.08)', marginBottom: '2rem'
        }}>
        <input placeholder="Nombre" value={form.nombre}
            onChange={e => setForm({ ...form, nombre: e.target.value })} required
            style={inputStyle} />
        <input placeholder="Descripción" value={form.descripcion}
            onChange={e => setForm({ ...form, descripcion: e.target.value })}
            style={inputStyle} />
        <input placeholder="Precio" type="number" step="0.01" value={form.precio}
            onChange={e => setForm({ ...form, precio: e.target.value })} required
            style={inputStyle} />
        <input placeholder="URL de imagen" value={form.imagenUrl}
            onChange={e => setForm({ ...form, imagenUrl: e.target.value })}
            style={inputStyle} />
        <select value={form.categoriaId}
            onChange={e => setForm({ ...form, categoriaId: e.target.value })} required
            style={inputStyle}>
            <option value="">Categoría</option>
            {categorias.map(c => <option key={c.id} value={c.id}>{c.nombre}</option>)}
        </select>
        <input placeholder="Stock mínimo" type="number" value={form.stockMinimo}
            onChange={e => setForm({ ...form, stockMinimo: e.target.value })}
            style={inputStyle} />
        <div style={{ display: 'flex', gap: '0.5rem' }}>
            <button type="submit" style={btnPrimary}>
            {editando ? 'Actualizar' : 'Crear'}
            </button>
            {editando && (
            <button type="button" onClick={() => {
                setEditando(null)
                setForm({ nombre: '', descripcion: '', precio: '', imagenUrl: '', categoriaId: '', stockMinimo: 5 })
            }} style={btnSecondary}>Cancelar</button>
            )}
        </div>
        </form>
        <table style={{ width: '100%', borderCollapse: 'collapse', background: '#fff', borderRadius: '8px', overflow: 'hidden' }}>
        <thead>
            <tr style={{ background: '#f5f5f5', textAlign: 'left' }}>
            <th style={thStyle}>Nombre</th>
            <th style={thStyle}>Precio</th>
            <th style={thStyle}>Categoría</th>
            <th style={thStyle}></th>
            </tr>
        </thead>
        <tbody>
            {productos.map(p => (
            <tr key={p.id} style={{ borderBottom: '1px solid #eee' }}>
                <td style={tdStyle}>{p.nombre}</td>
                <td style={tdStyle}>S/ {p.precio}</td>
                <td style={tdStyle}>{p.categoria.nombre}</td>
                <td style={tdStyle}>
                <button onClick={() => editar(p)} style={btnSmall}>Editar</button>
                <button onClick={() => desactivar(p.id)} style={{ ...btnSmall, marginLeft: '0.4rem', color: '#c62828' }}>Desactivar</button>
                </td>
            </tr>
            ))}
        </tbody>
        </table>
    </div>
    )
}

export default ProductosAdmin