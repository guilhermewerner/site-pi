using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using SitePi.Models;

namespace SitePi.Controllers
{
    public class HomeController : Controller
    {
        private AppContext AppContext;
        
        public HomeController(AppContext AppContext)
        {
            this.AppContext = AppContext;
        }
       
        public IActionResult Index()
        {
            ViewBag.Bots = AppContext.Bots;
            ViewBag.Users = AppContext.Users;

            return View();
        }

        public IActionResult Privacy()
        {
            return View();
        }
        
        public FileResult BaixarBot(int id)
        {
            Bot bot = AppContext.Bots.Find(id);
            
            byte[] FileBytes = System.IO.File.ReadAllBytes(bot.FileBytes);
            string FileName = bot.FileName;
            return File(FileBytes, System.Net.Mime.MediaTypeNames.Application.Octet, FileName);
        }

        public IActionResult DeleteBot(int id)
        {
            Bot bot = AppContext.Bots.Find(id);

            if (bot != null)
            {
                AppContext.Bots.Remove(bot);
                AppContext.SaveChanges();
            }

            return RedirectToAction("Index");
        }

        [ResponseCache(Duration = 0, Location = ResponseCacheLocation.None, NoStore = true)]
        public IActionResult Error()
        {
            return View(new ErrorViewModel { RequestId = Activity.Current?.Id ?? HttpContext.TraceIdentifier });
        }
    }
}
