/* ajustes: deitado (controle) ou em pé (toque), conforme a largura. */
import { emPe } from "../comum/tela.js";

if (emPe()) await import("./em-pe.js");
else await import("./deitado.js");
