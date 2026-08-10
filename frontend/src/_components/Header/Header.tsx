import Categories from "../Categories/Categories";

export function Header() {
  return (
    <>
      <header className="flex flex-col">
        <span className="p-font font-extralight text-muted text-xs">
          THE DAILY LEDGER
        </span>
        <div className="flex justify-between items-center">
          <span className="text-7xl italic">Todos.</span>
          <div className="flex flex-col items-end">
            <span className="italic text-accent text-xl">x of x done</span>
            <span className="text-muted ">x still open</span>
          </div>
        </div>
        <div className="border"></div>
      </header>
      <Categories />
    </>
  );
}
