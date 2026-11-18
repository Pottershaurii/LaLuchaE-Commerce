import { useState } from 'react'
import Categorias from './Categorias.jsx'
import ProductosAdmin from './ProductosAdmin.jsx'

function AdminLayout() {
  const [seccion, setSeccion] = useState('productos')

  const boton = (id, texto) => (
    <button
      onClick={() => setSeccion(id)}
      style={{
        display: 'block',
        width: '100%',
        textAlign: 'left',
        background: seccion === id ? '#c45c26' : 'transparent',
        color: '#fff',
        border: 'none',
        padding: '0.75rem 1rem',
        marginBottom: '0.25rem',
        borderRadius: '6px',
        cursor: 'pointer',
        fontSize: '0.95rem'
      }}
    >
      {texto}
    </button>
  )

  return (
    <div style={{ display: 'flex', minHeight: '100vh', fontFamily: 'system-ui, sans-serif' }}>
      <aside style={{
        width: '240px',
        background: '#1a1a1a',
        color: '#fff',
        padding: '1.5rem 1rem',
        display: 'flex',
        flexDirection: 'column'
      }}>
        <div style={{ marginBottom: '2rem', paddingLeft: '0.5rem' }}>
          <div style={{ fontSize: '1.25rem', fontWeight: 700 }}>La Lucha</div>
          <div style={{ fontSize: '0.75rem', color: '#999' }}>Panel de administración</div>
        </div>
        <nav>
          {boton('productos', 'Productos')}
          {boton('categorias', 'Categorías')}
        </nav>
      </aside>
      <main style={{ flex: 1, padding: '2rem', background: '#faf8f5' }}>
        {seccion === 'productos' && <ProductosAdmin />}
        {seccion === 'categorias' && <Categorias />}
      </main>
    </div>
  )
}

export default AdminLayout