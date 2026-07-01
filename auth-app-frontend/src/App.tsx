
import './App.css'
import { Button } from './components/ui/button'

function App() {
  

  return (
   <>  
     <div className='p-10'>
       <h1 className='text-3xl font-bold'>
         Hello aditya billioanire
       </h1>
       <Button variant={'secondary'}>Click Me 1</Button>
       <Button variant={'link'}>Click Me 2</Button>
       <Button >Click Me 3</Button>
       <Button variant={'ghost'}>Click Me 4</Button>
       <Button variant={'secondary'}>Click Me 5</Button>

      </div>
   </>
  )
}

export default App
