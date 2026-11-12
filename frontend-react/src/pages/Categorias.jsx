import { useEffect, useState } from 'react'

function Categorias() {
  const [categorias, setCategorias] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    fetch('http://localhost:8080/api/categorias')
      .then(res => {
        if (!res.ok) throw new Error('No se pudo cargar')
        return res.json()
      })
      .then(data => {
        setCategorias(data)
        setCargando(false)
      })
      .catch(() => {
        setError('No se pudieron cargar las categorías')
        setCargando(false)
      })
  }, [])

  if (cargando) return <p>Cargando categorías…</p>
  if (error) return <p>{error}</p>

  return (
    <div style={{ padding: '2rem' }}>
      <h1>Categorías</h1>
      <ul>
        {categorias.map(c => (
          <li key={c.id}>
            <strong>{c.nombre}</strong> ({c.codigo}) — {c.descripcion}
          </li>
        ))}
      </ul>
    </div>
  )
}

export default Categorias