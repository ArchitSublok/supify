import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { createBrowserRouter, RouterProvider } from 'react-router-dom'
import App from './app/App'
import { UserRoleProvider } from './entities/user/model/UserRoleContext'
import './app/styles.css'

const router = createBrowserRouter([{ path: '*', element: <App /> }])

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <UserRoleProvider>
      <RouterProvider router={router} />
    </UserRoleProvider>
  </StrictMode>,
)
